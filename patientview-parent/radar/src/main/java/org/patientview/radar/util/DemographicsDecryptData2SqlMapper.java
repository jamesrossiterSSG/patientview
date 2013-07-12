package org.patientview.radar.util;

import org.apache.commons.lang.StringUtils;
import org.patientview.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * {@link #run(javax.servlet.ServletContext)} gets the encrypted field data from demographics table, decrypts it,
 *      and creates an .sql file with the update statements for setting the decrypted values.
 *
 * Please make sure the folders in the {@link #FILE} exists.
 */
public class DemographicsDecryptData2SqlMapper {

    protected JdbcTemplate jdbcTemplate;

    private static final String FILE = "/radarunencrypteddemograpicstableexport/output/decrypted_demographics_data.sql";

    private static final String DATE_FORMAT = "dd.MM.y";
    private static final String DATE_FORMAT_2 = "dd-MM-y";
    private static final String DATE_FORMAT_3 = "dd/MM/y";

    private static final Logger LOGGER = LoggerFactory.getLogger(DemographicsDecryptData2SqlMapper.class);

    public void run(ServletContext servletContext) throws Exception {
        WebApplicationContext webApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(
                servletContext);

        jdbcTemplate = new JdbcTemplate((DataSource) webApplicationContext.getBean("dataSource"));

        List<Patient> patientList = jdbcTemplate.query("SELECT * FROM TBL_DEMOGRAPHICS",
                new EncryptedDemographicsRowMapper());

        StringBuilder outputText = new StringBuilder();
        for (Patient patient : patientList) {
            String updateStatement = "UPDATE TBL_DEMOGRAPHICS SET ";

            if (patient.getNhsno() != null) {
                updateStatement += " NHS_NO = '" + patient.getNhsno() + "', ";
            }

            if (patient.getHospitalnumber() != null) {
                updateStatement += " HOSP_NO = \"" + patient.getHospitalnumber() + "\", ";
            }

            if (patient.getSurname() != null) {
                updateStatement += " SNAME = \"" + patient.getSurname() + "\", ";
            }

            if (patient.getForename() != null) {
                updateStatement += " FNAME = \"" + patient.getForename() + "\", ";
            }

            if (patient.getSnameAlias() != null) {
                updateStatement += " SNAME_ALIAS = \"" + patient.getSnameAlias() + "\", ";
            }

            if (patient.getDob() != null) {
                // just guess what a sane date format is
                updateStatement += " DOB = \""
                        + new SimpleDateFormat(DATE_FORMAT_2).format(patient.getDob()) + "\", ";
            }

            if (patient.getAddress1() != null) {
                updateStatement += " ADD1 = \"" + patient.getAddress1() + "\", ";
            }

            if (patient.getAddress2() != null) {
                updateStatement += " ADD2 = \"" + patient.getAddress2() + "\", ";
            }

            if (patient.getAddress3() != null) {
                updateStatement += " ADD3 = \"" + patient.getAddress3() + "\", ";
            }

            if (patient.getAddress4() != null) {
                updateStatement += " ADD4 = \"" + patient.getAddress4() + "\", ";
            }

            if (patient.getPostcode() != null) {
                updateStatement += " POSTCODE = \"" + patient.getPostcode() + "\", ";
            }

            if (patient.getPostcodeOld() != null) {
                updateStatement += " POSTCODE_OLD = \"" + patient.getPostcodeOld() + "\", ";
            }

            updateStatement += " RADAR_NO = " + patient.getId();
            updateStatement += " WHERE RADAR_NO = " + patient.getId();
            updateStatement += " ;";

            outputText.append(updateStatement);
        }

        // output all sql stuff to file
        FileWriter fileWriter = new FileWriter(FILE);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(outputText.toString());
        //Close the output stream
        bufferedWriter.close();
    }

    private class EncryptedDemographicsRowMapper implements RowMapper<Patient> {
        public Patient mapRow(ResultSet resultSet, int i) throws SQLException {
            Patient patient = new Patient();
            patient.setId(resultSet.getLong("RADAR_NO"));

            try {
                patient.setNhsno(getDecryptedString(patient.getId() + "", "NHS_NO",
                        resultSet.getBytes("NHS_NO")));
                patient.setHospitalnumber(getDecryptedString(patient.getId() + "", "HOSP_NO",
                        resultSet.getBytes("HOSP_NO")));
                patient.setSurname(getDecryptedString(patient.getId() + "", "SNAME",
                        resultSet.getBytes("SNAME")));
                patient.setSnameAlias(getDecryptedString(patient.getId() + "", "SNAME_ALIAS",
                        resultSet.getBytes("SNAME_ALIAS")));
                patient.setForename(getDecryptedString(patient.getId() + "", "FNAME",
                        resultSet.getBytes("FNAME")));

                // Date needs to be decrypted to string, then parsed
                String dateOfBirthString = getDecryptedString(patient.getId() + "", "DOB",
                        resultSet.getBytes("DOB"));

                if (StringUtils.isNotBlank(dateOfBirthString)) {
                    Date dateOfBirth = getDate(dateOfBirthString, DATE_FORMAT);

                    if (dateOfBirth == null) {
                        dateOfBirth = getDate(dateOfBirthString, DATE_FORMAT_2);
                    }

                    if (dateOfBirth == null) {
                        dateOfBirth = getDate(dateOfBirthString, DATE_FORMAT_3);
                    }

                    // If after trying those formats we don't have anything then log as error
                    if (dateOfBirth != null) {
                        patient.setDob(dateOfBirth);
                    } else {
                        LOGGER.error("Could not parse date of birth from any format for dob {}",
                                dateOfBirthString);
                    }
                }

                // Addresses, all encrypted too
                patient.setAddress1(getDecryptedString(patient.getId() + "", "ADD1",
                        resultSet.getBytes("ADD1")));
                patient.setAddress2(getDecryptedString(patient.getId() + "", "ADD2",
                        resultSet.getBytes("ADD2")));
                patient.setAddress3(getDecryptedString(patient.getId() + "", "ADD3",
                        resultSet.getBytes("ADD3")));
                patient.setAddress4(getDecryptedString(patient.getId() + "", "ADD4",
                        resultSet.getBytes("ADD4")));
                patient.setPostcode(getDecryptedString(patient.getId() + "", "POSTCODE",
                        resultSet.getBytes("POSTCODE")));
                patient.setPostcodeOld(getDecryptedString(patient.getId() + "", "POSTCODE_OLD",
                        resultSet.getBytes("POSTCODE_OLD")));

            } catch (Exception e) {
                LOGGER.error("Could not decrypt demographics information for demographics {}", patient.getId());
                e.printStackTrace();
            }

            return patient;
        }
    }

    private String getDecryptedString(String radarNo, String fieldName, byte[] fieldData) throws Exception {
        if (fieldData != null && fieldData.length > 0) {
            try {
                byte[] copy = Arrays.copyOf(fieldData, fieldData.length);
                return TripleDes.decrypt(copy);
            } catch (Exception e) {
                LOGGER.error("Could not decrypt demographics information for radarNo {}, field {}, field data {}, " +
                        "message {}",
                        new Object[] {radarNo, fieldName, fieldData, e.getMessage()});
            }
        }

        return null;
    }

    private Date getDate(String dobStr, String dateFormat) {
        // It seems that the encrypted strings in the DB have different date formats, nice.
        try {
            return new SimpleDateFormat(dateFormat).parse(dobStr);
        } catch (Exception e) {
            // cya
        }
        return null;
    }
}
