# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"Thực Phẩm Xanh" — a single-module Android app (Java, package `com.UngDung.ThucPhamXanh`, minSdk 28 / target 35, Java 8 source level) for selling green food. Backend is entirely Firebase (Auth, Firestore, Realtime Database, Storage). Code comments and UI strings are in Vietnamese. Git remote: `https://github.com/nguyentrunghuu/thucphamsach` (public, branch `main`).

## Commands

Run from the repo root (on Windows use `gradlew.bat` or `./gradlew` from Git Bash):

```
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # build + install on connected device/emulator
./gradlew testDebugUnitTest             # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "com.UngDung.ThucPhamXanh.OrderStatusTest"   # single test class
./gradlew testDebugUnitTest --tests "*ShippingFeeHelperTest.someMethod"            # single test method
./gradlew connectedAndroidTest          # instrumented tests (needs device)
```

`testOptions.unitTests.returnDefaultValues = true` lets unit tests touch `android.util.Log` etc. Existing unit tests cover the pure helpers (`MoneyUtil`, `OrderStatus`, `ProductSearch`, `ShippingFeeHelper`) — keep new business logic in such pure helpers so it stays testable.

The loose `VnpayTest*.java`, `scratch.java`, `testHash.java` and `.class` files in the repo root are throwaway experiments, not part of the build.

## Configuration

- `local.properties` (not in git) holds `OPENAI_API_KEY` and `GEMINI_API_KEY`, injected as `BuildConfig.*`. Never hard-code API keys in source — the GitHub repo is public.
- `app/google-services.json` holds the Firebase project config for project `thucphamsach-144b3` (the `.bak-*` files beside it are old configs; `appai-a9aa1` is no longer accessible).
- Debug builds are signed with the project's own `app/debug.keystore` (`signingConfigs.debug` in `app/build.gradle`), SHA-1 `C9:7B:C2:86:F9:96:A0:94:C4:C2:C8:CD:26:AD:E3:BD:DB:75:C2:04`, registered in Firebase for Google Sign-In. Do not switch back to `~/.android/debug.keystore` — its SHA-1 is locked by the old `appai` project's OAuth client and Google Sign-In fails with code 10. A release build needs its own SHA-1 added in Firebase.
- Gradle 8.13 cannot run on JDK 25 (`Unsupported class file major version 69`); build with JDK 21 (e.g. `JAVA_HOME=C:\Users\AD\.jdks\jbr-21.0.11`, or set Android Studio's Gradle JDK to jbr-21).

## Architecture

### Roles and entry flow
`SplashActivity` → `SignInActivity` (or `MainActivity` if already signed in). Sign-in decides the role by comparing credentials against the Firestore `Admin` and `Shipper` collections (`tk`: 1 = admin, 3 = shipper, otherwise customer) and routes to:
- **Customer**: `MainActivity` hosting bottom-nav fragments (`fragment/` — Home, Cart, Bill, Chat, Favorite, Notify, Profile).
- **Admin**: `View/Admin/*` (products, categories, orders, charts/statistics, users, shippers, promotions).
- **Shipper**: `View/Shipper/*` (assigned orders, status updates).

Customer auth uses Firebase Auth (email, Google, Facebook via `Helper/SocialAuthHelper`).

### MVP-ish layering (customer side)
`my_interface/` defines contracts (`IXxx` = presenter callbacks, `XxxView` = view callbacks) → `Presenter/` implements them → `Models/` classes double as **data-access objects**: e.g. `Models/Giohang`, `Models/HoaDon`, `Models/Product` both hold fields and run the Firestore queries (`HandleReadData`, `HandleGetDataProduct`, …), returning results through the presenter interface. Admin/Shipper screens mostly query Firestore directly from the Activity.

### Firestore data layout (key collections)
- `SanPham` (products), `LoaiProduct` (categories), `Promotions`/`KhuyenMai`, `Banner`, `Story`, `ThongTinCuaHang`.
- Per-user data is nested under the user's UID with an `ALL` subcollection, e.g. `GioHang/{uid}/ALL`, `Favorite/{uid}/ALL`; order line items are `ChitietHoaDon/{orderId}/ALL`.
- `HoaDon` (orders), `BinhLuan` (reviews), `User`/`Profile`/`IDUser` (user info), `Chats` (customer↔admin chat; chat list in Realtime DB `Chatlist`).

### Shared rules that must be respected everywhere
- **Order status**: always use `Models/OrderStatus` constants/labels/colors for the `trangthai` field — never raw ints. Flow: 1 Đã đặt → 2 Đang xử lý → 5 Đã xác nhận → 4 Đang giao → 3 Đã giao; customer may cancel (→ 6) only while 1 or 2 (`customerCanCancel`); 3 and 6 are final (`isFinal`).
- **Product deletion is soft**: admin "delete" sets `daxoa = true`. Every customer-facing query must skip these via `Helper/ProductVisibility.isHidden(...)`.
- **Admin chat ID**: use `Helper/AppConstants.ADMIN_ID`; never hard-code it.
- **Reviews** (`BinhLuan` docs: `idproduct` = SanPham doc id, `iduser`, `noidung`, `rate` stored as a *string* like "5", "4.0", "2.5"): always read `rate` through `Helper/ReviewUtil.parseRate` (null/invalid-safe), format with `formatRate`, bucket with `starBucket`, and show reviewer name/avatar via `ReviewUtil.bindReviewer` (falls back from Firestore `User/{uid}/Profile` to Realtime `Users/{uid}` — Google/Facebook accounts often have no Profile).
- **Expiry date** `hansudung` on `SanPham` is a string `dd/MM/yyyy`. A separate `soNgayHSD` (shelf-life days) field exists on many docs but is unused by code and not kept in sync.
- **Admin order status changes** go through `Helper/AdminOrderFlow` (`nextStatuses`/`canTransition`): forward only, no skipping past "Đang giao", admin may cancel only in 1/2/5.
- **Admin UI**: all `View.Admin.*` activities use `Theme.ThucPhamXanh.Admin` (Manifest) with `admin_*` colors (`colors.xml`), `Admin.*` styles (`themes.xml`), `dimens_admin.xml`, icons `drawable/ic_admin_*`. New/redone admin layouts must use these, never hard-coded colors (old admin layouts still hard-code blue `#2196F3`). Admin activities are `exported="false"`.
- Dashboard numbers / stock & expiry warnings via `Helper/DashboardStats` (low stock ≤ 5, expiring ≤ 2 days); document → model via `Helper/FirestoreMapper` (null-safe).
- Money formatting via `Helper/MoneyUtil`; shipping fee via `Helper/ShippingFeeHelper` (distance from the fixed store location in Hà Nội: free within 5 km, otherwise same-province / different-province flat fees); Vietnamese diacritic-insensitive search via `Helper/TextNormalizer` + `Helper/ProductSearch`.

### Other subsystems
- `BotAI/`: AI chatbot (`ChatBotActivity`, Retrofit clients for Gemini/OpenAI) that builds a prompt from live product and order data. `BotAI/MyApplication` is the `Application` class (registered in the manifest) — it creates the notification channel and starts `NotificationService`, which listens for order-status changes and posts local notifications (`OrderStatusReceiver`, `NotificationUtils`).
- Payments: VNPay (`CauhinhAPI/`, `Helper/VNPayHelper`, `View/VNPayPaymentActivity`) and the MoMo SDK.
- Maps: OSMDroid (`MapLocationOSMActivity`) is the primary map; Google Maps variants remain. QR scanning via ZXing (`View/CaptureAct`). Admin statistics use MPAndroidChart and Excel export via Apache POI.

## Testing on a device

A physical phone is usually attached over USB. `adb` lives at `D:\android\androidsdk\platform-tools\adb.exe`. Typical loop: build with JDK 21 → `adb install -r app/build/outputs/apk/debug/app-debug.apk` → launch with `adb shell monkey -p com.UngDung.ThucPhamXanh -c android.intent.category.LAUNCHER 1` → `adb exec-out screencap -p > <scratchpad>/x.png` (use Bash, not PowerShell, for the binary redirect) and drive the UI with `adb shell input tap/text/swipe`. Search is diacritic-insensitive, so `input text "tieu"` finds "Tiêu đen".

Firestore can be read (and, currently, written) over REST with the API key from `google-services.json`: `https://firestore.googleapis.com/v1/projects/thucphamsach-144b3/databases/(default)/documents/<Collection>?pageSize=300&key=<key>`. Back up documents before any bulk write.

## Known issues / open follow-ups

- **Firestore security rules are wide open**: unauthenticated REST calls with the public API key can write `SanPham` (verified 2026-11-12). Rules need tightening.
- **No expiry enforcement**: products past `hansudung` are still listed and purchasable; fresh items set on 2026-11-12 expire again within 2–5 days.
- Some products are in the wrong category (Rau răm, Chanh, Hành lá, Lá lốt, Dứa, Ớt chuông, Me are under "Đồ khô").
- The review-submit dialog in `CTHDActivity` was fixed but not yet tested on a device (needs an order in status Đã giao).
- Repo root has throwaway files (`VnpayTest*.java/.class`, `scratch.*`, `testHash.java`, `hs_err_pid*.log`, `replay_pid*.log`) that can be deleted.

## Work log

**2026-11-12**
- Created this CLAUDE.md.
- **Google Sign-In made to work** (code in `Helper/SocialAuthHelper` already existed; config was broken):
  - Firebase project is `thucphamsach-144b3`; Google provider enabled; Android app `com.UngDung.ThucPhamXanh` registered with the project keystore SHA-1/SHA-256; new `google-services.json` downloaded (now contains a `client_type: 1` Android OAuth client).
  - `strings.xml` `google_web_client_id` fallback changed from the old `appai` project's client to `465388708652-9pl5uvu2pdaf4m1jtgnbgjtdi1dcesun.apps.googleusercontent.com`.
  - Added `app/debug.keystore` + `signingConfigs.debug` in `app/build.gradle` (old default keystore SHA-1 was locked by the inaccessible `appai` project). Verified login on the phone.
- **Product expiry data** (Firestore `SanPham`, data only, no code change): 52 products had empty `hansudung`, 22 were expired. Set `hansudung` for all 74 based on product type, counted from 2026-11-12 (live shellfish 2 days, fish/shrimp/squid 3, pork 4, beef 5, minced/offal 2, poultry 3, leafy herbs 4–5, eggs 30 (quail 20, balut 3), roots 30–60, dried beans 12 months, peanuts 6 months, seasonings/oil/fish sauce 18–36 months). Đậu Trắng and Đậu Xanh were still valid and left untouched.
- **Reviews ("Đánh giá và nhận xét") fixed and verified on the phone:**
  - `View/CommentActivity`: rewritten — single query; average no longer shows "NaN" when there are 0 reviews; star distribution bars now count reviewers per star bucket (previously summed star values against a max of 100); empty state text `tv_empty_cmt`.
  - `layout/activity_comment.xml`: summary RatingBar was scaled 0.26× and clipped → `Widget.AppCompat.RatingBar.Small` with gold tint; added `tv_empty_cmt`.
  - `layout/dong_all_comment.xml`: per-review RatingBar was oversized/clipped → small style; container height `wrap_content`.
  - `Adapter/AllCommentAdapter`, `Adapter/BinhLuanAdapter`: use `ReviewUtil` (safe rate parsing, `"x.x"` formatting, reviewer name/avatar with fallbacks, no stale data on recycled rows; replaced the "Nguyễn Văn Chính" placeholder with "Người dùng").
  - `View/CTHDActivity` review dialog: previously reused the last dialog's rating/text via shared fields (`danhgia`, `r`, now removed); now reads the views on submit, requires a comment choice and rating > 0, disables the button while sending, handles failure.
  - `Adapter/HoaDonDaGiaoAdapter`: "Đã đánh giá" state is reset per bind and queried per (user, product), so it no longer leaks to other rows.
  - New `Helper/ReviewUtil.java` + `test/.../ReviewUtilTest.java` (all 20 unit tests pass).
- **Pushed to GitHub** `https://github.com/nguyentrunghuu/thucphamsach` (public, branch `main`). Before pushing: moved the Gemini key from `BotAI/ChatBotActivity` and `View/MessageActivity` into `local.properties` → `BuildConfig.GEMINI_API_KEY`; `.gitignore` now excludes `app/google-services.json`, `app/debug.keystore`, `local.properties`, `.idea/`, build output and root test/log junk. A fresh clone therefore needs those three files copied in by hand (the keystore is optional for building — `build.gradle` falls back to the default debug key, but Google Sign-In then fails with code 10).

**2026-11-13 — Admin redesign, plan + steps 0–1**
- Plan (8 steps): 0 style base → 1 pure helpers → 2 `BaseAdminActivity` + shared toolbar/empty views → 3 new dashboard (revenue today/month, pending orders, low stock, expiring) → 4 product list (search, category chips, `ListAdapter`/DiffUtil, one listener) → 5 add/edit product (`TextInputLayout`, new optional field `xuatxu`) → 6 orders (status chips, search, own `AdminOrderAdapter` — do NOT change customer `HoaDonAdapter`; one shared status BottomSheet replacing the two duplicated `DiaLogUpDate`) → 7 categories/users/shippers/promotions → 8 charts + cleanup.
- Done step 0: admin colors/styles/dimens/icons, `res/color/admin_chip_*.xml`, `bg_admin_badge`, `bg_admin_icon_box`; Manifest gives admin activities the admin theme and `exported="false"` (they were `exported="true"`, so any app could open `AdminHomeActivity` without login).
- Done step 1: `Helper/AdminOrderFlow`, `Helper/DashboardStats`, `Helper/FirestoreMapper` + `AdminOrderFlowTest`, `DashboardStatsTest` (35 unit tests pass, debug build OK). Helpers not wired into screens yet.
- Checked on the phone (logged in as admin): Home, Hóa đơn, Sản phẩm, Edit sản phẩm, Người dùng, Thống kê, Khuyến mãi, Loại SP, Shipper all open without crash under the new theme; they look the same as before because the old layouts hard-code their own colors.
- Pre-existing UI defects seen (fix in steps 4/5/8): calendar icon in `activity_admin_add_spactivity.xml` is a solid blue square (`drawableTint` on the opaque PNG `icons8_calendar`); header says "Edit sản phẩm / Tạo sản phẩm mới" in edit mode; product rows wrap "SL" digits vertically and squeeze names ("Đậu p / hộng"); revenue on the home card wraps ("2.058. / 250"); the pie chart in Thống kê is tiny with overlapping labels.
- Done step 2: `View/Admin/BaseAdminActivity` (Firestore listeners attached in `onStartListening()` via `listen(...)`, auto-removed in `onStop`; `setupAdminToolbar`; `applySystemBarInsets` — **targetSdk 35 forces edge-to-edge on Android 15+, so admin screens must pad for the status/nav bars themselves**; `confirm`; `showEmpty`), `layout/view_admin_toolbar.xml`, `layout/view_admin_empty.xml`. Only `AdminHomeActivity` extends it so far.
- Done step 3: new dashboard (`AdminHomeActivity`, `activity_admin_home.xml` 778 → ~300 lines, `item_admin_stat`, `item_admin_menu`, `item_admin_attention`, `AdminAttentionAdapter` = `ListAdapter`/DiffUtil). Shows revenue today / this month (delivered orders by `ngaydat`), pending orders (1+2), low-stock count, menu grid, and a "Cần chú ý" list (expired/out of stock/expiring/low stock, most urgent first, 5 shown + "Xem tất cả"). Reads: 1 listener `HoaDon`, 1 listener `SanPham`, `count()` on `IDUser`. Sign-out asks for confirmation and clears the back stack. `MoneyUtil.formatVnd/formatNumber` added. Verified on the phone (39 unit tests pass).
- Data facts seen 2026-11-13: of 112 orders only 8 are Đã giao (none in 11/2026, so month revenue shows 0 đ correctly), 62 are stuck in Đã xác nhận and 32 in Đang giao; 30 products expire within 2 days.
- Project uses recyclerview 1.1 (no `getBindingAdapterPosition`, use `getAdapterPosition`). No Python on this machine.
- Known admin bugs to fix in later steps: `AdminShipperActivity.deleteShipper` removes by stale `position`; `AdminCTHDActivity` updates UI before Firestore write succeeds; `layout/dong_cthd_admin.xml` is empty/unused.
- Step 4 (product list), 2026-11-13: `AdminProductActivity` rewritten on `BaseAdminActivity`: shared toolbar (subtitle = product count), diacritic-insensitive search box, status chips (Tất cả / Sắp hết hàng / Hết hạn–sắp hết hạn), category chips with counts built from `LoaiProduct`, ExtendedFAB "Thêm sản phẩm" that shrinks on scroll, empty/loading states. Reads: 1 listener `SanPham` (all, filtered on the device) + 1 listener `LoaiProduct`. Soft-deleted products stay hidden. Mapping goes through `FirestoreMapper.toProduct`, which fixes the null-unboxing crash. `AdminProductAdapter` is now a `ListAdapter`/DiffUtil with `layout/item_admin_product.xml` (thumb, name 2 lines, category · weight · HSD, price, stock badge always shown, expiry badge when ≤ 2 days/expired). Removed `dong_admin_product.xml`. New pure `Helper/AdminProductFilter` + `AdminProductFilterTest` (47 unit tests pass). Dashboard "Sắp hết hàng" card opens the list pre-filtered (`AdminProductActivity.intent(ctx, STATUS_LOW_STOCK)`). Filter changes scroll the list to the top (ListAdapter otherwise keeps the old anchor row). Verified on the phone: pre-filter, status/category chips, search "bo", tap → edit screen. Admin login for device testing: see memory (not stored in the repo).
