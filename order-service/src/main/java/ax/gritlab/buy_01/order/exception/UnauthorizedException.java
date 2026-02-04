package ax.gritlab.buy_01.order.exception;

/**
 * Exception thrown when user is not authorized to perform an action.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
}
