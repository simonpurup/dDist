package Project;

/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class TextInsertEvent extends MyTextEvent {

	private String text;
	
	public TextInsertEvent(int offset, String text) {
		super(offset);
		this.text = text;
	}
	public String getText() { return text; }

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof TextInsertEvent) {
			TextInsertEvent event = (TextInsertEvent) obj;
			if(event.getOffset() == getOffset() && event.getText().equals(getText()))
				return true;
			else
				return false;
		}
		else
			return false;
	}

	@Override
	public String toString() {
		return text + " " + super.getOffset();
	}
}

