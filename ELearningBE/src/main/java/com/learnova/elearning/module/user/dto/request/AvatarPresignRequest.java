package com.learnova.elearning.module.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarPresignRequest {

    @NotBlank(message = "Tên file không được để trống")
    private String fileName;

    @NotBlank(message = "Loại nội dung (contentType) không được để trống")
    private String contentType;

    @NotNull(message = "Kích thước file không được để trống")
    @Min(value = 1, message = "Kích thước file phải lớn hơn 0")
    @Max(value = 5L * 1024 * 1024, message = "Kích thước ảnh đại diện tối đa là 5MB")
    private Long sizeBytes;
}
