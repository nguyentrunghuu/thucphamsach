package com.UngDung.ThucPhamXanh.Helper;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Helper class để tính phí ship dựa trên khoảng cách thực tế
 * - Dưới 5km: Free ship
 * - Trên 5km: Tính phí (20,000 VNĐ cho cùng tỉnh, 30,000 VNĐ cho khác tỉnh)
 */
public class ShippingFeeHelper {
    
    // Địa chỉ cửa hàng (điểm xuất phát để tính khoảng cách / phí ship)
    private static final String STORE_ADDRESS = "Trường Đại học Điện lực, 235 Hoàng Quốc Việt, Bắc Từ Liêm, Hà Nội";
    private static final String STORE_PROVINCE = "Hà Nội"; // Tỉnh/Thành phố cửa hàng (dùng để xét "cùng tỉnh")

    // Tọa độ cửa hàng: Trường Đại học Điện lực (cơ sở chính, 235 Hoàng Quốc Việt)
    // Muốn đổi vị trí: mở Google Maps, nhấn giữ vào điểm cần đặt, copy "vĩ độ, kinh độ" vào 2 dòng dưới.
    private static final double STORE_LATITUDE = 21.0466;
    private static final double STORE_LONGITUDE = 105.7853;

    // Phí ship
    private static final long FREE_SHIP = 0;
    private static final long SAME_PROVINCE_SHIP = 20000;
    private static final long DIFFERENT_PROVINCE_SHIP = 30000;
    
    // Bán kính free ship (km)
    private static final double FREE_SHIP_RADIUS_KM = 5.0;
    
    /**
     * Tính phí ship dựa trên địa chỉ người nhận
     * @param deliveryAddress Địa chỉ giao hàng
     * @return Phí ship (VNĐ)
     */
    public static long calculateShippingFee(String deliveryAddress) {
        return calculateShippingFee(null, deliveryAddress, null, null);
    }
    
    /**
     * Tính phí ship dựa trên địa chỉ và tọa độ (nếu có)
     * @param context Context để sử dụng Geocoder (có thể null)
     * @param deliveryAddress Địa chỉ giao hàng
     * @param latitude Latitude của địa chỉ giao hàng (nếu có)
     * @param longitude Longitude của địa chỉ giao hàng (nếu có)
     * @return Phí ship (VNĐ)
     */
    public static long calculateShippingFee(Context context, String deliveryAddress, Double latitude, Double longitude) {
        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            // Nếu chưa có địa chỉ, trả về phí ship mặc định (khác tỉnh)
            Log.d("ShippingFeeHelper", "Chưa có địa chỉ, trả về phí ship mặc định: " + DIFFERENT_PROVINCE_SHIP);
            return DIFFERENT_PROVINCE_SHIP;
        }
        
        double distance = -1;
        
        // Nếu có tọa độ, tính khoảng cách trực tiếp
        if (latitude != null && longitude != null) {
            distance = calculateDistance(STORE_LATITUDE, STORE_LONGITUDE, latitude, longitude);
            Log.d("ShippingFeeHelper", "Khoảng cách từ tọa độ: " + distance + " km");
        } else if (context != null) {
            // Nếu không có tọa độ nhưng có context, thử lấy từ địa chỉ
            distance = getDistanceFromAddress(context, deliveryAddress);
            Log.d("ShippingFeeHelper", "Khoảng cách từ địa chỉ: " + distance + " km");
        }
        
        // Nếu tính được khoảng cách
        if (distance >= 0) {
            if (distance <= FREE_SHIP_RADIUS_KM) {
                Log.d("ShippingFeeHelper", "Khoảng cách " + distance + " km <= " + FREE_SHIP_RADIUS_KM + " km, free ship");
                return FREE_SHIP;
            } else {
                // Trên 5km, kiểm tra cùng tỉnh hay khác tỉnh
                if (isSameProvinceAsStore(deliveryAddress)) {
                    Log.d("ShippingFeeHelper", "Khoảng cách " + distance + " km > " + FREE_SHIP_RADIUS_KM + " km, cùng tỉnh, phí ship: " + SAME_PROVINCE_SHIP);
                    return SAME_PROVINCE_SHIP;
                } else {
                    Log.d("ShippingFeeHelper", "Khoảng cách " + distance + " km > " + FREE_SHIP_RADIUS_KM + " km, khác tỉnh, phí ship: " + DIFFERENT_PROVINCE_SHIP);
                    return DIFFERENT_PROVINCE_SHIP;
                }
            }
        }
        
        // Fallback: Nếu không tính được khoảng cách, dựa vào tỉnh/thành trong địa chỉ
        if (isSameProvinceAsStore(deliveryAddress)) {
            Log.d("ShippingFeeHelper", "Cùng tỉnh (fallback), phí ship: " + SAME_PROVINCE_SHIP);
            return SAME_PROVINCE_SHIP;
        } else {
            Log.d("ShippingFeeHelper", "Khác tỉnh (fallback), phí ship: " + DIFFERENT_PROVINCE_SHIP);
            return DIFFERENT_PROVINCE_SHIP;
        }
    }
    
    /**
     * Tính khoảng cách giữa hai điểm tọa độ (Haversine formula)
     * @param lat1 Latitude điểm 1
     * @param lon1 Longitude điểm 1
     * @param lat2 Latitude điểm 2
     * @param lon2 Longitude điểm 2
     * @return Khoảng cách (km)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Bán kính Trái Đất (km)
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        double distance = R * c;
        
        Log.d("ShippingFeeHelper", String.format(
            "calculateDistance: Điểm 1: (%.6f, %.6f), Điểm 2: (%.6f, %.6f), Khoảng cách: %.2f km",
            lat1, lon1, lat2, lon2, distance
        ));
        
        return distance;
    }
    
    /**
     * Lấy khoảng cách từ shop đến địa chỉ giao hàng
     * @param context Context để sử dụng Geocoder (có thể null)
     * @param deliveryAddress Địa chỉ giao hàng
     * @param latitude Latitude của địa chỉ giao hàng (nếu có)
     * @param longitude Longitude của địa chỉ giao hàng (nếu có)
     * @return Khoảng cách (km), -1 nếu không tính được
     */
    public static double getDistanceToStore(Context context, String deliveryAddress, Double latitude, Double longitude) {
        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            Log.w("ShippingFeeHelper", "getDistanceToStore: Địa chỉ rỗng");
            return -1;
        }
        
        // Nếu có tọa độ, tính khoảng cách trực tiếp (ưu tiên dùng tọa độ từ map)
        if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
            double distance = calculateDistance(STORE_LATITUDE, STORE_LONGITUDE, latitude, longitude);
            Log.d("ShippingFeeHelper", String.format(
                "getDistanceToStore: Dùng tọa độ từ map - Cửa hàng: (%.6f, %.6f), Địa chỉ giao: (%.6f, %.6f), Khoảng cách: %.2f km",
                STORE_LATITUDE, STORE_LONGITUDE, latitude, longitude, distance
            ));
            return distance;
        } else if (context != null) {
            // Nếu không có tọa độ nhưng có context, thử lấy từ địa chỉ (geocoding)
            Log.d("ShippingFeeHelper", "getDistanceToStore: Không có tọa độ từ map, dùng geocoding cho địa chỉ: " + deliveryAddress);
            return getDistanceFromAddress(context, deliveryAddress);
        }
        
        Log.w("ShippingFeeHelper", "getDistanceToStore: Không thể tính khoảng cách");
        return -1;
    }
    
    /**
     * Lấy khoảng cách từ địa chỉ bằng cách geocode
     * @param context Context
     * @param address Địa chỉ
     * @return Khoảng cách (km), -1 nếu không lấy được
     */
    private static double getDistanceFromAddress(Context context, String address) {
        if (context == null || address == null || address.trim().isEmpty()) {
            Log.w("ShippingFeeHelper", "getDistanceFromAddress: Context hoặc địa chỉ rỗng");
            return -1;
        }
        
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                double lat = addr.getLatitude();
                double lon = addr.getLongitude();
                double distance = calculateDistance(STORE_LATITUDE, STORE_LONGITUDE, lat, lon);
                Log.d("ShippingFeeHelper", String.format(
                    "getDistanceFromAddress: Geocoded địa chỉ '%s' -> (%.6f, %.6f), Khoảng cách: %.2f km",
                    address, lat, lon, distance
                ));
                return distance;
            } else {
                Log.w("ShippingFeeHelper", "getDistanceFromAddress: Không tìm thấy địa chỉ: " + address);
            }
        } catch (IOException e) {
            Log.e("ShippingFeeHelper", "Error geocoding address: " + e.getMessage(), e);
        }
        
        return -1;
    }
    
    /**
     * Kiểm tra xem địa chỉ có gần cửa hàng không
     * Có thể cải thiện bằng cách sử dụng API tính khoảng cách thực tế
     */
    private static boolean isNearStore(String address) {
        if (address == null || address.trim().isEmpty()) {
            return false;
        }
        
        String normalizedAddress = normalizeAddress(address);
        
        // Danh sách các quận/huyện gần cửa hàng (có thể mở rộng)
        String[] nearDistricts = {
            "ba đình", "hoàn kiếm", "tây hồ", "long biên", "cầu giấy",
            "đống đa", "hai bà trưng", "hoàng mai", "thanh xuân"
        };
        
        // Kiểm tra xem địa chỉ có chứa tên quận/huyện gần cửa hàng không
        for (String district : nearDistricts) {
            if (normalizedAddress.contains(district)) {
                return true;
            }
        }
        
        // Có thể thêm logic kiểm tra khoảng cách thực tế nếu có tọa độ
        return false;
    }
    
    /**
     * Chuẩn hóa địa chỉ để so sánh: chữ thường, BỎ DẤU tiếng Việt, chỉ giữ chữ/số,
     * các khoảng trắng gộp thành 1. Ví dụ: "Hà Nội," -> "ha noi", "TP.HCM" -> "tp hcm".
     */
    static String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        String s = address.toLowerCase(Locale.ROOT).replace('đ', 'd');
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");               // bỏ dấu
        s = s.replaceAll("[^a-z0-9]+", " ").trim();       // ký tự khác -> khoảng trắng
        return s;
    }

    /**
     * Địa chỉ giao hàng có cùng tỉnh/thành với cửa hàng không.
     * Không nhận ra được tỉnh/thành -> trả về false (tính phí khác tỉnh), tránh tính thiếu phí.
     */
    public static boolean isSameProvinceAsStore(String deliveryAddress) {
        String userProvince = extractProvince(deliveryAddress);
        if (userProvince.isEmpty()) {
            Log.d("ShippingFeeHelper", "Không xác định được tỉnh/thành từ địa chỉ: " + deliveryAddress);
            return false;
        }
        return normalizeAddress(userProvince).equals(normalizeAddress(STORE_PROVINCE));
    }
    
    /**
     * Lấy tên tỉnh/thành phố từ địa chỉ
     * Địa chỉ thường có format: "Số nhà, Tên đường, Phường/Xã, Quận/Huyện, Tỉnh/Thành phố"
     */
    public static String extractProvince(String address) {
        if (address == null || address.trim().isEmpty()) {
            return "";
        }
        
        // Danh sách các tỉnh/thành phố phổ biến ở Việt Nam
        String[] provinces = {
            "Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
            "An Giang", "Bà Rịa - Vũng Tàu", "Bạc Liêu", "Bắc Giang", "Bắc Kạn",
            "Bắc Ninh", "Bến Tre", "Bình Định", "Bình Dương", "Bình Phước",
            "Bình Thuận", "Cà Mau", "Cao Bằng", "Đắk Lắk", "Đắk Nông",
            "Điện Biên", "Đồng Nai", "Đồng Tháp", "Gia Lai", "Hà Giang",
            "Hà Nam", "Hà Tĩnh", "Hải Dương", "Hậu Giang", "Hòa Bình",
            "Hưng Yên", "Khánh Hòa", "Kiên Giang", "Kon Tum", "Lai Châu",
            "Lâm Đồng", "Lạng Sơn", "Lào Cai", "Long An", "Nam Định",
            "Nghệ An", "Ninh Bình", "Ninh Thuận", "Phú Thọ", "Phú Yên",
            "Quảng Bình", "Quảng Nam", "Quảng Ngãi", "Quảng Ninh", "Quảng Trị",
            "Sóc Trăng", "Sơn La", "Tây Ninh", "Thái Bình", "Thái Nguyên",
            "Thanh Hóa", "Thừa Thiên Huế", "Tiền Giang", "Trà Vinh", "Tuyên Quang",
            "Vĩnh Long", "Vĩnh Phúc", "Yên Bái"
        };
        
        String normalizedAddress = normalizeAddress(address);

        // Cách viết tắt / tên gọi khác thường gặp
        String[][] aliases = {
            {"tphcm", "Hồ Chí Minh"}, {"tp hcm", "Hồ Chí Minh"}, {"hcm", "Hồ Chí Minh"},
            {"sai gon", "Hồ Chí Minh"}, {"saigon", "Hồ Chí Minh"},
            {"hn", "Hà Nội"}, {"tp hn", "Hà Nội"},
            {"hue", "Thừa Thiên Huế"}, {"vung tau", "Bà Rịa - Vũng Tàu"},
        };

        // Tìm tỉnh/thành phố trong địa chỉ (có dấu hoặc không dấu, có/không khoảng trắng).
        // Tỉnh/thành thường đứng CUỐI địa chỉ, nên chọn tên xuất hiện sau cùng
        // (tránh nhầm tên phố/phường trùng tên tỉnh, vd "phố Thái Bình, Hà Nội").
        String padded = " " + normalizedAddress + " ";
        String best = "";
        int bestPos = -1;
        for (String province : provinces) {
            String p = normalizeAddress(province);                         // "ha noi"
            for (String variant : new String[]{p, p.replace(" ", "")}) {   // "hanoi"
                int pos = padded.lastIndexOf(" " + variant + " ");
                if (pos > bestPos) { bestPos = pos; best = province; }
            }
        }
        for (String[] alias : aliases) {
            int pos = padded.lastIndexOf(" " + alias[0] + " ");
            if (pos > bestPos) { bestPos = pos; best = alias[1]; }
        }

        return best;
    }
    
    /**
     * Tính phí ship dựa trên tỉnh/thành phố
     * @param userProvince Tỉnh/thành phố của người nhận
     * @return Phí ship (VNĐ)
     */
    public static long calculateShippingFeeByProvince(String userProvince) {
        if (userProvince == null || userProvince.trim().isEmpty()) {
            return DIFFERENT_PROVINCE_SHIP;
        }
        
        String normalizedUserProvince = normalizeAddress(userProvince);
        String normalizedStoreProvince = normalizeAddress(STORE_PROVINCE);
        
        if (normalizedUserProvince.equals(normalizedStoreProvince)) {
            return SAME_PROVINCE_SHIP;
        } else {
            return DIFFERENT_PROVINCE_SHIP;
        }
    }
    
    /**
     * Lấy địa chỉ cửa hàng
     */
    public static String getStoreAddress() {
        return STORE_ADDRESS;
    }
    
    /**
     * Lấy tỉnh/thành phố cửa hàng
     */
    public static String getStoreProvince() {
        return STORE_PROVINCE;
    }
    
    /**
     * Lấy tọa độ cửa hàng (để debug)
     */
    public static double[] getStoreCoordinates() {
        return new double[]{STORE_LATITUDE, STORE_LONGITUDE};
    }
    
    /**
     * Geocode địa chỉ cửa hàng để lấy tọa độ chính xác (dùng để cập nhật tọa độ)
     * @param context Context để sử dụng Geocoder
     * @return double[] {latitude, longitude} hoặc null nếu không geocode được
     */
    public static double[] geocodeStoreAddress(Context context) {
        if (context == null) {
            Log.e("ShippingFeeHelper", "geocodeStoreAddress: Context null");
            return null;
        }
        
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(STORE_ADDRESS, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                double lat = addr.getLatitude();
                double lon = addr.getLongitude();
                Log.d("ShippingFeeHelper", String.format(
                    "geocodeStoreAddress: Geocoded '%s' -> (%.6f, %.6f)",
                    STORE_ADDRESS, lat, lon
                ));
                return new double[]{lat, lon};
            } else {
                Log.w("ShippingFeeHelper", "geocodeStoreAddress: Không tìm thấy địa chỉ: " + STORE_ADDRESS);
            }
        } catch (IOException e) {
            Log.e("ShippingFeeHelper", "geocodeStoreAddress: Error geocoding: " + e.getMessage(), e);
        }
        
        return null;
    }
}

