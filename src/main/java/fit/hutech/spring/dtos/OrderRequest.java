package fit.hutech.spring.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {
    private String receiverName;
    private String phoneNumber;
    private String address;
    private String note;
    private String paymentMethod;
}
