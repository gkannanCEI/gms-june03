package com.gms.dto;

import java.util.List;

public class BulkRoundSetupRequest {

    private Long programId;
    private String roundName;
    private List<PageSetup> pages;

    public Long getProgramId() { return programId; }
    public void setProgramId(Long programId) { this.programId = programId; }
    public String getRoundName() { return roundName; }
    public void setRoundName(String roundName) { this.roundName = roundName; }
    public List<PageSetup> getPages() { return pages; }
    public void setPages(List<PageSetup> pages) { this.pages = pages; }

    public static class PageSetup {
        private String pageName;
        private String pageDescription;
        private List<QuestionSetup> questions;

        public String getPageName() { return pageName; }
        public void setPageName(String pageName) { this.pageName = pageName; }
        public String getPageDescription() { return pageDescription; }
        public void setPageDescription(String pageDescription) { this.pageDescription = pageDescription; }
        public List<QuestionSetup> getQuestions() { return questions; }
        public void setQuestions(List<QuestionSetup> questions) { this.questions = questions; }
    }

    public static class QuestionSetup {
        private String label;
        private String questionType;
        private String validationRegex;
        private String minValue;
        private String maxValue;
        private boolean required;
        private List<OptionSetup> options;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getQuestionType() { return questionType; }
        public void setQuestionType(String questionType) { this.questionType = questionType; }
        public String getValidationRegex() { return validationRegex; }
        public void setValidationRegex(String validationRegex) { this.validationRegex = validationRegex; }
        public String getMinValue() { return minValue; }
        public void setMinValue(String minValue) { this.minValue = minValue; }
        public String getMaxValue() { return maxValue; }
        public void setMaxValue(String maxValue) { this.maxValue = maxValue; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public List<OptionSetup> getOptions() { return options; }
        public void setOptions(List<OptionSetup> options) { this.options = options; }
    }

    public static class OptionSetup {
        private String optionLabel;
        private String optionValue;
        private boolean isOtherOption;

        public String getOptionLabel() { return optionLabel; }
        public void setOptionLabel(String optionLabel) { this.optionLabel = optionLabel; }
        public String getOptionValue() { return optionValue; }
        public void setOptionValue(String optionValue) { this.optionValue = optionValue; }
        public boolean isIsOtherOption() { return isOtherOption; }
        public void setIsOtherOption(boolean isOtherOption) { this.isOtherOption = isOtherOption; }
    }
}
