package fit.hutech.spring.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {
    private String senderName;
    private String receiverName;
    private String phoneNumber;
    private String address;
    private String note;
    private String paymentMethod;
    private java.util.List<Long> itemIds; // List of book IDs to checkout
}
