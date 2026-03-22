package com.medo.cards.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardsDto {
    @NotEmpty(message = "Mobile number can not be null or empty")
    @Pattern(regexp="(^$|[0-9]{10})",message = "Mobile Number must be 10 digits")
    private String mobileNumber;
    @NotEmpty(message = "Card number can not be null or empty")
    @Pattern(regexp="(^$|[0-9]{12})",message = "Card Number must be 12 digits")
    private String cardNumber;
    @NotEmpty(message = "Card Type can not be null or empty")
    private String cardType;
    @Positive(message = "Total card limit should be greater than zero")
    private int totalLimit;
    @PositiveOrZero(message = "Total amount used should be equal or greater than zero")
    private int amountUsed;
    @PositiveOrZero(message = "Total available amount should be equal or greater than zero")
    private int availableAmount;
}
