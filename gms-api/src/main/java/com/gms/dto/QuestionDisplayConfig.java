package com.gms.dto;

public class QuestionDisplayConfig {
    private boolean readonly;
    private int columnSpan = 12;
    private String labelPosition = "ABOVE";
    private String helpText;
    private String sectionGroup;

    public boolean isReadonly() { return readonly; }
    public void setReadonly(boolean readonly) { this.readonly = readonly; }
    public int getColumnSpan() { return columnSpan; }
    public void setColumnSpan(int columnSpan) { this.columnSpan = columnSpan; }
    public String getLabelPosition() { return labelPosition; }
    public void setLabelPosition(String labelPosition) { this.labelPosition = labelPosition; }
    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }
    public String getSectionGroup() { return sectionGroup; }
    public void setSectionGroup(String sectionGroup) { this.sectionGroup = sectionGroup; }
}
