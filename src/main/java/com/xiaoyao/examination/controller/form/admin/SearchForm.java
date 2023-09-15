package com.xiaoyao.examination.controller.form.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SearchForm {
    @NotNull
    @Min(1)
    private long page;

    @NotNull
    @Min(1)
    private long size;

    private String name;
}
