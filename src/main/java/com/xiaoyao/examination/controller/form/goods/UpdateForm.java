package com.xiaoyao.examination.controller.form.goods;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class UpdateForm {
    @NotNull
    @Min(1)
    private long id;

    private String name;

    private String code;

    private String description;

    @Pattern(regexp = "^(\\d+)\\.(\\d{2})$")
    private String originalPrice;

    @Pattern(regexp = "^(\\d+)\\.(\\d{2})$")
    private String currentPrice;

    private Long discountId;

    private String image;

    @Min(1)
    private Integer type;

    private List<String> tag;

    private List<CreateForm.Item> departmentCheckup;

    private List<CreateForm.Item> laboratoryCheckup;

    private List<CreateForm.Item> medicalCheckup;

    private List<CreateForm.Item> otherCheckup;

    @Data
    public static class Item {
        private String name;
        private String description;
    }
}