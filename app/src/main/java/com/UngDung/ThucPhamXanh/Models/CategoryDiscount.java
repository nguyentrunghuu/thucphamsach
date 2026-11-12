package com.UngDung.ThucPhamXanh.Models;

public class CategoryDiscount {
    private int minProducts;      // Số sản phẩm tối thiểu cần mua
    private int discountPercent;  // % giảm giá
    
    public CategoryDiscount() {
        this.minProducts = 0;
        this.discountPercent = 0;
    }
    
    public CategoryDiscount(int minProducts, int discountPercent) {
        this.minProducts = minProducts;
        this.discountPercent = discountPercent;
    }
    
    public int getMinProducts() {
        return minProducts;
    }
    
    public void setMinProducts(int minProducts) {
        this.minProducts = minProducts;
    }
    
    public int getDiscountPercent() {
        return discountPercent;
    }
    
    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }
}

