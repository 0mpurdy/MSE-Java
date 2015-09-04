import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

class StopException extends Exception {
    StopException () {
        super();
    }

    StopException (String message) {
        super(message);
    }
}//StopException
