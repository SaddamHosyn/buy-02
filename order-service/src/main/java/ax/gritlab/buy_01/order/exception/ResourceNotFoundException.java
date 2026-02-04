package ax.gritlab.buy_01.order.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceType, String id) {
        super(String.format("%s not found with id: %s", resourceType, id));
    }
}
