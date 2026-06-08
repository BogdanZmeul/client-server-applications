package db;

import java.util.List;
import java.util.Objects;

public class ProductPage {
    private List<Product> products;
    private int totalCount;
    private int page;
    private int pageSize;

    public ProductPage(List<Product> products, int totalCount, int page, int pageSize) {
        this.products = products;
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProductPage that = (ProductPage) o;
        return totalCount == that.totalCount && page == that.page && pageSize == that.pageSize && Objects.equals(products, that.products);
    }

    @Override
    public int hashCode() {
        return Objects.hash(products, totalCount, page, pageSize);
    }

    @Override
    public String toString() {
        return "ProductPage{" +
                "products=" + products +
                ", totalCount=" + totalCount +
                ", page=" + page +
                ", pageSize=" + pageSize +
                '}';
    }
}
