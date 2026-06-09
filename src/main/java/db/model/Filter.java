package db.model;

import java.util.List;

public class Filter {
    public String name;
    public List<String> groups;
    public Integer minCount;
    public Integer maxCount;
    public Double minPrice;
    public Double maxPrice;
    public Integer page;
    public Integer pageSize;
}
