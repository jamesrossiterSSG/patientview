package com.worthsoln.repository;

import com.worthsoln.patientview.model.TestResultWithUnitShortname;
import com.worthsoln.patientview.model.Panel;
import com.worthsoln.patientview.model.Unit;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 *
 */
@Transactional(propagation = Propagation.MANDATORY)
public interface TestResultDao {

    // Get the test results for the patient for the units they belong to.
    // The unit list is per tenancy.
    List<TestResultWithUnitShortname> getTestResultForPatient(String username, Panel panel, List<Unit> units);
}