package ax.gritlab.buy_01.order.exception;

public class CheckoutValidationException extends RuntimeException {
    public CheckoutValidationException(String message) {
        super(message);
    }
}
