package dDist.Project;

import Project.DistributedTextEditor;
import Project.MyTextEvent;

import javax.swing.*;
import java.util.LinkedList;

/**
 * Created by Simon on 29-05-2017.
 */
public class DistributedTextEditorStub extends DistributedTextEditor{

    public DistributedTextEditorStub() {
    }

    @Override
    public JTextArea getArea(){
        return null;
    }

    @Override
    public LinkedList<MyTextEvent> getEventsPerformed(){
        return null;
    }
}
