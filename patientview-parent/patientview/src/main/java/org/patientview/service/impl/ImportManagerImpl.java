/*
 * PatientView
 *
 * Copyright (c) Worth Solutions Limited 2004-2013
 *
 * This file is part of PatientView.
 *
 * PatientView is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * PatientView is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with PatientView in a file
 * titled COPYING. If not, see <http://www.gnu.org/licenses/>.
 *
 * @package PatientView
 * @link http://www.patientview.org
 * @author PatientView <info@patientview.org>
 * @copyright Copyright (c) 2004-2013, Worth Solutions Limited
 * @license http://www.gnu.org/licenses/gpl-3.0.html The GNU General Public License V3.0
 */

package org.patientview.service.impl;

import org.patientview.ibd.model.Allergy;
import org.patientview.ibd.model.MyIbd;
import org.patientview.ibd.model.Procedure;
import org.patientview.model.Patient;
import org.patientview.model.Unit;
import org.patientview.model.enums.SourceType;
import org.patientview.patientview.TestResultDateRange;
import org.patientview.patientview.XmlImportUtils;
import org.patientview.patientview.logging.AddLog;
import org.patientview.patientview.model.Centre;
import org.patientview.patientview.model.Diagnosis;
import org.patientview.patientview.model.Diagnostic;
import org.patientview.patientview.model.Letter;
import org.patientview.patientview.model.Medicine;
import org.patientview.patientview.model.FootCheckup;
import org.patientview.patientview.model.EyeCheckup;
import org.patientview.patientview.model.TestResult;
import org.patientview.patientview.parser.ResultParser;
import org.patientview.patientview.user.UserUtils;
import org.patientview.patientview.utils.TimestampUtils;
import org.patientview.quartz.exception.ProcessException;
import org.patientview.quartz.exception.ResultParserException;
import org.patientview.quartz.handler.ErrorHandler;
import org.patientview.repository.UnitDao;
import org.patientview.service.ImportManager;
import org.patientview.service.LogEntryManager;
import org.patientview.util.CommonUtils;
import org.patientview.utils.LegacySpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
@Service(value = "importManager")
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ProcessException.class)
public class ImportManagerImpl implements ImportManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportManagerImpl.class);

    @Inject
    private XmlImportUtils xmlImportUtils;

    @Inject
    private UnitDao unitDao;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private LogEntryManager logEntryManager;

    @Inject
    private ErrorHandler errorHandler;


    @Override
    public Unit retrieveUnit(String unitCode) {
        unitCode = unitCode.toUpperCase();
        return unitDao.get(unitCode, null);
    }



    public void process(File xmlFile) throws ProcessException {

        LOGGER.debug("Processing file {}.", xmlFile.getName());

        if (xmlFile.length() == 0) {
            errorHandler.emptyFile(xmlFile);
            throw new ProcessException("The file is empty");
        }

        ResultParser resultParser = null;

        try {
            resultParser = new ResultParser(xmlFile);
        } catch (ResultParserException pe) {
            errorHandler.parserException(xmlFile, pe);
            throw new ProcessException("Could not create the parser for the file", pe);
        }

        // If the file parse process otherwise email the corruptions
        if (resultParser.parse()) {

            String action = null;

            try {
                action = processPatientData(resultParser);
            } catch (ProcessException pe) {
                errorHandler.processingException(xmlFile, pe);
                throw pe;
            } catch (Exception e) {
                errorHandler.processingException(xmlFile, e);
                throw new ProcessException("There has been an error processing the data", e);
            }

            log(xmlFile, action);

        } else {
            errorHandler.corruptNodes(xmlFile, resultParser);
            throw new ProcessException("There are file corruptions");
        }
    }

    private boolean hasPatientLeft(ResultParser parser) {
        return ("Remove".equalsIgnoreCase(parser.getFlag()) || "Dead".equalsIgnoreCase(parser.getFlag())
                || "Died".equalsIgnoreCase(parser.getFlag()) || "Lost".equalsIgnoreCase(parser.getFlag())
                || "Suspend".equalsIgnoreCase(parser.getFlag()));
    }

    private void removePatientFromSystem(ResultParser parser) {
        UserUtils.removePatientFromSystem(parser.getData("nhsno"), parser.getData("centrecode"));
    }

    private String processPatientData(ResultParser resultParser) throws ProcessException {
        if (hasPatientLeft(resultParser)) {
            removePatientFromSystem(resultParser);
            return AddLog.PATIENT_DATA_REMOVE;
        } else {
            validateNhsNumber(resultParser.getPatient());
            validateUnitCode(resultParser.getCentre());
            updatePatientDetails(resultParser.getPatient(), resultParser.getDateRanges());
            deleteDateRanges(resultParser.getDateRanges());
            insertResults(resultParser.getTestResults());
            deleteLetters(resultParser.getLetters());
            insertLetters(resultParser.getLetters());
            deleteOtherDiagnoses(resultParser.getData("nhsno"), resultParser.getData("centrecode"));
            insertOtherDiagnoses(resultParser.getOtherDiagnoses());
            deleteMedicines(resultParser.getData("nhsno"), resultParser.getData("centrecode"));
            insertMedicines(resultParser.getMedicines());
            deleteMyIbd(resultParser.getData("nhsno"), resultParser.getData("centrecode"));
            insertMyIbd(resultParser.getMyIbd());
            deleteDiagnostics(resultParser.getData("nhsno"), resultParser.getData("centrecode"));
            insertDiagnostics(resultParser.getDiagnostics());
            deleteProcedures(resultParser.getData("nhsno"), resultParser.getData("centrecode"));
            insertProcedures(resultParser.getProcedures());
            deleteAllergies(resultParser.getData("nhsno"), resultParser.getData("centrecode"));
            insertAllergies(resultParser.getAllergies());
            deleteFootCheckup(resultParser.getData("nhsno"), resultParser.getData("centrecode"));
            insertFootCheckup(resultParser.getFootCheckupses());
            deleteEyeCheckup(resultParser.getData("nhsno"), resultParser.getData("centrecode"));
            insertEyeCheckup(resultParser.getEyeCheckupses());
            // todo improvement: we should build a set of all units updated, then mark them at the end of the job
            markLastImportDateOnUnit(resultParser.getCentre());

            return AddLog.PATIENT_DATA_FOLLOWUP;
        }
    }

    private void markLastImportDateOnUnit(Centre centre) {
        Unit unit = LegacySpringUtils.getImportManager().retrieveUnit(centre.getCentreCode());
        if (unit != null) {
            unit.setLastImportDate(new Date());
            unitDao.save(unit);
        }
    }

    private void deleteDiagnostics(String nhsno, String unitcode) {
        LegacySpringUtils.getDiagnosticManager().delete(nhsno, unitcode);
    }

    private void insertDiagnostics(Collection<Diagnostic> diagnostics) {
        for (Iterator iterator = diagnostics.iterator(); iterator.hasNext();) {
            Diagnostic diagnostic = (Diagnostic) iterator.next();
            LegacySpringUtils.getDiagnosticManager().save(diagnostic);
        }
    }

    private void deleteProcedures(String nhsno, String unitcode) {
        LegacySpringUtils.getIbdManager().deleteProcedure(nhsno, unitcode);
    }

    private void insertProcedures(Collection<Procedure> procedures) {
        for (Iterator iterator = procedures.iterator(); iterator.hasNext();) {
            Procedure procedure = (Procedure) iterator.next();
            LegacySpringUtils.getIbdManager().saveProcedure(procedure);
        }
    }

    private void deleteAllergies(String nhsno, String unitcode) {
        LegacySpringUtils.getIbdManager().deleteAllergy(nhsno, unitcode);
    }

    private void insertAllergies(Collection<Allergy> allergies) {
        for (Iterator iterator = allergies.iterator(); iterator.hasNext();) {
            Allergy allergy = (Allergy) iterator.next();
            LegacySpringUtils.getIbdManager().saveAllergy(allergy);
        }
    }

    private void deleteMyIbd(String nhsno, String unitcode) {
        LegacySpringUtils.getIbdManager().deleteMyIbd(nhsno, unitcode);
    }

    private void insertMyIbd(MyIbd myIbd) {
        if (myIbd != null) {
            LegacySpringUtils.getIbdManager().saveMyIbd(myIbd);
        }
    }

    /**
     *
     *  If we have test results that are later than any seen before,
     *  update the patient mostRecentTestResultDateRangeStopDate.
     *
     *  Only update the mostRecentTestResultDateRangeStopDate if the new values is after the
     *  existing value on the existing patient record
     *
     * @param patient new patient details
     * @param dateRanges the date ranges for test results found in this import
     */
    private void updatePatientDetails(Patient patient, List<TestResultDateRange> dateRanges) throws ProcessException {

        Patient existingPatientRecord
                = LegacySpringUtils.getPatientManager().get(patient.getNhsno(), patient.getUnitcode());

        // This field should be not nullable.
        if (existingPatientRecord != null && existingPatientRecord.getSourceType() != null
                && existingPatientRecord.getSourceType().equals(SourceType.RADAR.getName())) {
            throw new ProcessException("Cannot update an existing Radar patient record");
        }

        Date existingTestResultDateRangeStopDate = null;
        if (existingPatientRecord != null && existingPatientRecord.hasValidId()) {
            existingTestResultDateRangeStopDate = existingPatientRecord.getMostRecentTestResultDateRangeStopDate();
        }

        patient.setMostRecentTestResultDateRangeStopDate(
                getMostRecentTestResultDateRangeStopDate(dateRanges, existingTestResultDateRangeStopDate));


        // Have to do it like this because Radar uses JDBC only
        patient.setSourceType(SourceType.PATIENT_VIEW.getName());

        if (existingPatientRecord != null) {
            LegacySpringUtils.getPatientManager().save(XmlImportUtils.copyObject(existingPatientRecord, patient));
        } else {
            LegacySpringUtils.getPatientManager().save(patient);
        }
    }

    private Date getMostRecentTestResultDateRangeStopDate(List<TestResultDateRange> dateRanges,
                                                          Date mostRecentTestResultDateRangeStopDate) {
        if (dateRanges != null && dateRanges.size() > 0) {
            for (TestResultDateRange testResultDateRange : dateRanges) {
                Date stopDate = TimestampUtils.createTimestampEndDay(testResultDateRange.getStopDate()).getTime();
                // update the most recent if after
                if (mostRecentTestResultDateRangeStopDate == null
                        || stopDate.after(mostRecentTestResultDateRangeStopDate)) {
                    mostRecentTestResultDateRangeStopDate = stopDate;
                }
            }
        }
        return mostRecentTestResultDateRangeStopDate;
    }

    private void validateNhsNumber(Patient patient) throws ProcessException {
        if (!CommonUtils.isNhsNumberValidWhenUppercaseLettersAreAllowed(patient.getNhsno())) {
            throw new ProcessException("The NHS number is not in a invalid format");
        }
    }


    private void validateUnitCode(Centre centre) throws ProcessException {

        if (unitDao.get(centre.getCentreCode(), null) == null) {
            throw new ProcessException("The unit code supplied by the file does not exist in the database");
        }

    }

    private void deleteDateRanges(Collection dateRanges) {
        for (Iterator iterator = dateRanges.iterator(); iterator.hasNext();) {
            TestResultDateRange testResultDateRange = (TestResultDateRange) iterator.next();

            Calendar startDate = TimestampUtils.createTimestampStartDay(testResultDateRange.getStartDate());
            Calendar stopDate = TimestampUtils.createTimestampEndDay(testResultDateRange.getStopDate());

            LegacySpringUtils.getTestResultManager().deleteTestResultsWithinTimeRange(testResultDateRange.getNhsNo(),
                    testResultDateRange.getUnitcode(), testResultDateRange.getTestCode(), startDate.getTime(),
                    stopDate.getTime());
        }
    }

    private void insertResults(Collection testResults) {
        for (Iterator iterator = testResults.iterator(); iterator.hasNext();) {
            TestResult testResult = (TestResult) iterator.next();
            LegacySpringUtils.getTestResultManager().save(testResult);
        }
    }

    private void deleteLetters(Collection letters) {


        for (Iterator iterator = letters.iterator(); iterator.hasNext();) {
            Letter letter = (Letter) iterator.next();

            // Avoiding NPE in RPV-126. Although this will leave the letter in the DB.
            if (letter.getDate() != null) {
            LegacySpringUtils.getLetterManager().delete(letter.getNhsno(), letter.getUnitcode(),
                    letter.getDate().getTime());
            } else {
                LOGGER.warn("The letter does not come with a date so skipping deletion");
            }
        }
    }

    private void insertLetters(Collection letters) {
        for (Iterator iterator = letters.iterator(); iterator.hasNext();) {
            Letter letter = (Letter) iterator.next();
            LegacySpringUtils.getLetterManager().save(letter);
        }
    }

    private void deleteOtherDiagnoses(String nhsno, String unitcode) {
        LegacySpringUtils.getDiagnosisManager().deleteOtherDiagnoses(nhsno, unitcode);
    }

    private void insertOtherDiagnoses(Collection diagnoses) {
        for (Iterator iterator = diagnoses.iterator(); iterator.hasNext();) {
            Diagnosis diagnosis = (Diagnosis) iterator.next();
            LegacySpringUtils.getDiagnosisManager().save(diagnosis);
        }
    }

    private void deleteMedicines(String nhsno, String unitcode) {
        LegacySpringUtils.getMedicineManager().delete(nhsno, unitcode);
    }

    private void insertMedicines(Collection medicines) {
        for (Iterator iterator = medicines.iterator(); iterator.hasNext();) {
            Medicine medicine = (Medicine) iterator.next();
            LegacySpringUtils.getMedicineManager().save(medicine);
        }
    }

    private void log(File xmlFile, String action) {
        errorHandler.createLogEntry(xmlFile, action, "");
    }


    private void deleteFootCheckup(String nhsno, String unitcode) {
        LegacySpringUtils.getFootCheckupManager().delete(nhsno, unitcode);
    }

    private void insertFootCheckup(Collection<FootCheckup> checkupses) {
        for (Iterator iterator = checkupses.iterator(); iterator.hasNext();) {
            FootCheckup checkups = (FootCheckup) iterator.next();
            LegacySpringUtils.getFootCheckupManager().save(checkups);
        }
    }

    private void deleteEyeCheckup(String nhsno, String unitcode) {
        LegacySpringUtils.getEyeCheckupManager().delete(nhsno, unitcode);
    }

    private void insertEyeCheckup(List<EyeCheckup> eyeCheckups) {
        for (EyeCheckup eyeCheckup : eyeCheckups) {
            LegacySpringUtils.getEyeCheckupManager().save(eyeCheckup);
        }
    }
}
