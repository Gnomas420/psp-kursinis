package lt.partijapro.web;

// not found OR belongs to another tenant - client can't tell the difference
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
