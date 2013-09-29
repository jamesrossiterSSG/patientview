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
package org.patientview.quartz;

import com.Ostermiller.util.CSVPrinter;
import org.patientview.model.Patient;
import org.patientview.patientview.FindXmlFiles;
import org.patientview.patientview.uktransplant.UktParserUtils;
import org.patientview.utils.LegacySpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

/**
 * Quartz XmlImportJobQuartzScheduler Job
 */
public class XmlImportJobQuartzScheduler {

    @Value("${run.import.export.threads}")
    private String runImport;

    @Value("${run.ukt.threads}")
    private String runUkt;

    @Value("${xml.directory}")
    private String xmlDirectory;

    @Value("${ukt.directory}")
    private String uktDirectory;

    @Value("${uktexport.directory}")
    private String uktExportDirectory;

    private String[] fileEndings = {".xml", };

    private static final int NUMBER_OF_COLUMNS = 5;

    private static final int THREE = 3;

    private static final int FOUR = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlImportJobQuartzScheduler.class);

    public void execute() {
        System.out.println("XmlImportJobQuartzScheduler is starting.");
        try {
            Thread.sleep(NUMBER_OF_COLUMNS * FOUR * NUMBER_OF_COLUMNS
                    *  NUMBER_OF_COLUMNS * FOUR * NUMBER_OF_COLUMNS);
        } catch (InterruptedException ire) {
            System.out.println("sssss");
        }
        try {
            updateXmlFiles();

            if ((runUkt == null) || !"false".equals(runUkt)) {
                File uktDir = new File(uktDirectory);
                File[] uktFiles = uktDir.listFiles(new UktFileFilter());
                if (uktFiles != null && uktFiles.length > 0) {
                    updateUktFiles(uktFiles);
                }

                File uktExportDir = new File(uktExportDirectory);
                File uktExportFile = new File(uktExportDir, "ukt_rpv_export.txt");
                if (uktExportFile != null && uktExportFile.isFile()) {
                    CSVPrinter csv = new CSVPrinter(new FileWriter(uktExportFile));
                    csv.setAlwaysQuote(true);
                    csv.writeln(getPatients());
                    csv.flush();
                    csv.close();
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.debug(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        System.out.println("XmlImportJobQuartzScheduler is ending.");
    }

    private void updateXmlFiles() {
        if ((runImport == null) || !"false".equals(runImport)) {
            File[] xmlFiles = FindXmlFiles.findXmlFiles(xmlDirectory, fileEndings);
            if (xmlFiles != null && xmlFiles.length > 0) {
                LOGGER.debug("Starting XmlParserThread for {} files", xmlFiles.length);
                for (int i = 0; i < xmlFiles.length; i++) {
                    LOGGER.debug("Starting XmlParserThread for {} file", xmlFiles[i].getAbsolutePath());
                    LegacySpringUtils.getImportManager().update(xmlFiles[i]);
                }
            }
        }

        LOGGER.info("updateXmlFiles Finished");
    }

    private void updateUktFiles(File[] uktFiles) throws IOException {
        LOGGER.debug("Starting UktParserThread for {} files", uktFiles.length);
        for (int i = 0; i < uktFiles.length; i++) {
            LOGGER.debug("Starting UktParserThread for {} file", uktFiles[i].getAbsolutePath());
            UktParserUtils.updateData(uktFiles[i]);
            uktFiles[i].delete();
        }
    }

    private String[][] getPatients() {
        List patientList = LegacySpringUtils.getPatientManager().getUktPatients();
        String[][] patientArray = new String[patientList.size()][NUMBER_OF_COLUMNS];
        for (int i = 0; i < patientList.size(); i++) {
            Patient patient = (Patient) patientList.get(i);
            patientArray[i][0] = (patient.getNhsno() == null) ? "" : patient.getNhsno();
            patientArray[i][1] = (patient.getSurname() == null) ? "" : patient.getSurname().replaceAll("\"", "");
            patientArray[i][2] = (patient.getForename() == null) ? "" : patient.getForename().replaceAll("\"", "");
            patientArray[i][THREE] = (patient.getDateofbirth() == null) ? "" : patient.getDateofbirth();
            patientArray[i][FOUR] = (patient.getPostcode() == null) ? "" : patient.getPostcode();
        }
        return patientArray;
    }

    public void setXmlDirectory(String xmlDirectory) {
        this.xmlDirectory = xmlDirectory;
    }

    public void setUktDirectory(String uktDirectory) {
        this.uktDirectory = uktDirectory;
    }

    public void setUktExportDirectory(String uktExportDirectory) {
        this.uktExportDirectory = uktExportDirectory;
    }


class UktFileFilter implements FilenameFilter {

    public boolean accept(File dir, String name) {
        return name.endsWith("uktstatus.gpg.txt");
    }
}


}
