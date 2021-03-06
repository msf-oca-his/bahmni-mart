package org.bahmni.mart.exports.template;

import org.bahmni.mart.config.job.JobDefinitionUtil;
import org.bahmni.mart.config.job.JobDefinitionValidator;
import org.bahmni.mart.config.job.model.CodeConfig;
import org.bahmni.mart.config.job.model.IncrementalUpdateConfig;
import org.bahmni.mart.config.job.model.JobDefinition;
import org.bahmni.mart.exports.updatestrategy.CustomSqlIncrementalUpdateStrategy;
import org.bahmni.mart.exports.updatestrategy.IncrementalStrategyContext;
import org.bahmni.mart.table.CodesProcessor;
import org.bahmni.mart.table.listener.TableGeneratorJobListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.batch.core.Job;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.bahmni.mart.CommonTestHelper.setValuesForMemberFields;
import static org.bahmni.mart.CommonTestHelper.setValuesForSuperClassMemberFields;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobDefinitionUtil.class, JobDefinitionValidator.class})
public class SimpleJobTemplateTest {

    @Mock
    private TableGeneratorJobListener listener;

    @Mock
    private Job job;

    @Mock
    private IncrementalUpdateConfig incrementalUpdateConfig;

    @Mock
    private CodesProcessor codesProcessor;

    @Mock
    private CustomSqlIncrementalUpdateStrategy customSqlIncrementalUpdater;

    @Mock
    private IncrementalStrategyContext incrementalStrategyContext;

    @Mock
    private JobDefinition jobDefinition;

    private SimpleJobTemplate spyJobTemplate;
    private String readerSql;
    private List<CodeConfig> codeConfigs;
    private static final String JOB_NAME = "testJob";
    private static final String TABLE_NAME = "tableName";
    private static final String JOB_TYPE = "jobType";


    @Before
    public void setUp() throws Exception {
        SimpleJobTemplate simpleJobTemplate = new SimpleJobTemplate();
        setValuesForMemberFields(simpleJobTemplate, "codesProcessor", codesProcessor);
        setValuesForMemberFields(simpleJobTemplate, "tableGeneratorJobListener", listener);
        setValuesForSuperClassMemberFields(simpleJobTemplate, "incrementalStrategyContext", incrementalStrategyContext);

        spyJobTemplate = spy(simpleJobTemplate);

        readerSql = "select * from table";
        CodeConfig codeConfig = mock(CodeConfig.class);
        codeConfigs = Arrays.asList(codeConfig);
        when(jobDefinition.getCodeConfigs()).thenReturn(codeConfigs);
        when(jobDefinition.getName()).thenReturn(JOB_NAME);
        when(jobDefinition.getTableName()).thenReturn(TABLE_NAME);
        when(jobDefinition.getType()).thenReturn(JOB_TYPE);
        when(jobDefinition.getIncrementalUpdateConfig()).thenReturn(incrementalUpdateConfig);
        when(incrementalUpdateConfig.getUpdateOn()).thenReturn("patient_id");
        mockStatic(JobDefinitionValidator.class);
        mockStatic(JobDefinitionUtil.class);

        when(incrementalStrategyContext.getStrategy(anyString())).thenReturn(customSqlIncrementalUpdater);
        when(customSqlIncrementalUpdater.isMetaDataChanged(anyString(), anyString())).thenReturn(true);
        doReturn(job).when((JobTemplate) spyJobTemplate).buildJob(jobDefinition, listener, readerSql);
        when(JobDefinitionUtil.getReaderSQL(jobDefinition)).thenReturn(readerSql);
        when(JobDefinitionUtil.getReaderSQLByIgnoringColumns(any(), anyString())).thenReturn(readerSql);
    }

    @Test
    public void shouldBuildJobDependsOnJobConfiguration() throws Exception {
        spyJobTemplate.buildJob(jobDefinition);

        verify(spyJobTemplate, times(1)).buildJob(jobDefinition, listener, readerSql);
        verifyStatic(times(1));
        JobDefinitionUtil.getReaderSQLByIgnoringColumns(Collections.emptyList(), readerSql);
        verify(jobDefinition).getCodeConfigs();
        verify(jobDefinition, atLeastOnce()).getType();
        verify(jobDefinition).getIncrementalUpdateConfig();
        verify(incrementalStrategyContext, atLeastOnce()).getStrategy(JOB_TYPE);
        verify(customSqlIncrementalUpdater, times(1)).isMetaDataChanged(TABLE_NAME, JOB_NAME);
    }

    @Test
    public void shouldSetPreProcessorGivenValidCodeConfigs() throws Exception {
        when(JobDefinitionValidator.isValid(codeConfigs)).thenReturn(true);

        spyJobTemplate.buildJob(jobDefinition);

        verify(codesProcessor, times(1)).setCodeConfigs(codeConfigs);
        verify(listener, times(1)).setCodesProcessor(codesProcessor);
        verify(spyJobTemplate, times(1)).setPreProcessor(codesProcessor);
        verify(customSqlIncrementalUpdater, times(1)).isMetaDataChanged(TABLE_NAME, JOB_NAME);
        verify(jobDefinition).getIncrementalUpdateConfig();
        verify(jobDefinition, atLeastOnce()).getType();
        verify(incrementalStrategyContext, atLeastOnce()).getStrategy(JOB_TYPE);
        verify(incrementalUpdateConfig, never()).getUpdateOn();
        verify(customSqlIncrementalUpdater).isMetaDataChanged(TABLE_NAME, JOB_NAME);
        verifyStatic(times(1));
        JobDefinitionValidator.isValid(codeConfigs);
    }

    @Test
    public void shouldNotSetPreprocessorGivenInvalidCodeConfigs() throws Exception {
        when(JobDefinitionValidator.isValid(codeConfigs)).thenReturn(false);

        spyJobTemplate.buildJob(jobDefinition);

        verify(customSqlIncrementalUpdater, times(1)).isMetaDataChanged(TABLE_NAME, JOB_NAME);
        verify(codesProcessor, never()).setUpCodesData();
        verify(spyJobTemplate, never()).setPreProcessor(codesProcessor);
        verify(jobDefinition).getIncrementalUpdateConfig();
        verify(incrementalUpdateConfig, never()).getUpdateOn();
        verify(jobDefinition, atLeastOnce()).getType();
        verify(incrementalStrategyContext, atLeastOnce()).getStrategy(JOB_TYPE);
        verify(customSqlIncrementalUpdater).isMetaDataChanged(TABLE_NAME, JOB_NAME);
        verifyStatic(times(1));
        JobDefinitionValidator.isValid(codeConfigs);

    }

    @Test
    public void shouldUpdateReaderSqlIfMetadataIsSame() {
        when(customSqlIncrementalUpdater.isMetaDataChanged(TABLE_NAME, JOB_NAME)).thenReturn(false);
        String updatedReaderSql = String.format("SELECT * FROM (%s) result where patient_id in (1, 2, 3)", readerSql);
        doReturn(job).when((JobTemplate) spyJobTemplate).buildJob(jobDefinition, listener, updatedReaderSql);
        String updateOn = "patient_id";
        when(customSqlIncrementalUpdater.updateReaderSql(readerSql, JOB_NAME, updateOn)).thenReturn(updatedReaderSql);

        spyJobTemplate.buildJob(jobDefinition);

        verify(spyJobTemplate, times(1)).buildJob(jobDefinition, listener, updatedReaderSql);
        verifyStatic(times(1));
        JobDefinitionUtil.getReaderSQLByIgnoringColumns(Collections.emptyList(), readerSql);
        verify(customSqlIncrementalUpdater, times(1)).isMetaDataChanged(TABLE_NAME, JOB_NAME);
        verify(customSqlIncrementalUpdater, times(1)).updateReaderSql(readerSql, JOB_NAME, updateOn);
        verify(jobDefinition).getIncrementalUpdateConfig();
        verify(incrementalUpdateConfig).getUpdateOn();
        verify(incrementalStrategyContext, atLeastOnce()).getStrategy(JOB_TYPE);
    }

    @Test
    public void shouldNotUpdateReaderSqlIfIncrementalUpdateConfigIsNotPresent() {
        when(jobDefinition.getIncrementalUpdateConfig()).thenReturn(null);

        spyJobTemplate.buildJob(jobDefinition);

        verify(spyJobTemplate).buildJob(jobDefinition, listener, readerSql);
        verify(jobDefinition).getIncrementalUpdateConfig();
        verify(incrementalUpdateConfig, never()).getUpdateOn();
        verify(customSqlIncrementalUpdater, never()).isMetaDataChanged(anyString(), anyString());
        verify(customSqlIncrementalUpdater, never()).updateReaderSql(anyString(), anyString(), anyString());
    }
}