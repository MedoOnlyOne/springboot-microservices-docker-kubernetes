package com.medo.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
@Schema(name = "Success response",
        description = "successful response schema"
)
public class ResponseDto {
    private String statusCode;
    private String statusMsg;
}
