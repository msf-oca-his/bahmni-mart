package org.bahmni.mart.table;

import org.apache.commons.lang3.StringUtils;
import org.bahmni.mart.form.domain.BahmniForm;
import org.bahmni.mart.form.domain.Concept;
import org.bahmni.mart.table.domain.ForeignKey;
import org.bahmni.mart.table.domain.TableColumn;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Primary
@Component("FormTableMetadataGenerator")
public class FormTableMetadataGenerator extends TableMetadataGenerator {

    @Override
    protected List<TableColumn> getColumns(BahmniForm form) {
        List<TableColumn> columns = new ArrayList<>();
        columns.add(getPrimaryColumn(form));
        columns.add(new TableColumn("patient_id", "integer", false, null));
        columns.add(new TableColumn("encounter_id", "integer", false, null));
        columns.add(new TableColumn("visit_id", "integer", false, null));
        columns.add(new TableColumn("obs_datetime", "timestamp", false, null));
        columns.add(new TableColumn("date_created", "timestamp", false, null));
        columns.add(new TableColumn("date_modified", "timestamp", false, null));
        columns.add(new TableColumn("location_id", "integer", false, null));
        columns.add(new TableColumn("location_name", "text", false, null));
        columns.add(new TableColumn("program_id", "integer", false, null));
        columns.add(new TableColumn("program_name", "text", false, null));
        columns.add(new TableColumn("patient_program_id", "integer", false, null));

        List<TableColumn> foreignKeyColumn = getForeignKeyColumns(form);
        if (foreignKeyColumn != null)
            columns.addAll(foreignKeyColumn);
        columns.addAll(getNonKeyColumns(form));
        return columns;
    }

    private TableColumn getPrimaryColumn(BahmniForm form) {
        return new TableColumn(String.format("id_%s", getProcessedName(form.getFormName().getName())),
                "integer", true, null);
    }

    @Override
    protected List<TableColumn> getForeignKeyColumns(BahmniForm form) {
        if (form.getParent() == null) {
            return null;
        }
        Concept formParentConcept = form.getParent().getFormName();
        String formParentConceptName = formParentConcept.getName();
        String referenceTableName = getProcessedName(formParentConceptName);

        referenceTableName = SpecialCharacterResolver.getUpdatedTableNameIfExist(referenceTableName);

        String referenceColumn = "id_" + referenceTableName;
        ForeignKey reference = new ForeignKey(referenceColumn, referenceTableName);

        return Arrays.asList(new TableColumn(referenceColumn, "integer", false, reference));
    }

    public static String addPrefixToName(String name, String prefix) {
        return StringUtils.isEmpty(prefix) ? name : String.format("%s %s", prefix, name);
    }
}
