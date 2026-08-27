package com.learnova.elearning.module.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
    private String fullName;

    @Size(max = 500, message = "Avatar key hoặc URL tối đa 500 ký tự")
    private String avatarKey;

    @Size(max = 2000, message = "Tiểu sử tối đa 2000 ký tự")
    private String bio;

    @Size(max = 255, message = "Chuyên môn tối đa 255 ký tự")
    private String expertise;

    /**
     * Danh sách các chủ đề/lĩnh vực quan tâm (sẽ được lưu dưới dạng JSON array)
     */
    private List<String> interests;
}
