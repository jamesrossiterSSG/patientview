package com.solidstategroup.radar.dao.impl;

import com.solidstategroup.radar.dao.DemographicsDao;
import com.solidstategroup.radar.dao.UtilityDao;
import com.solidstategroup.radar.model.Centre;
import com.solidstategroup.radar.model.Demographics;
import com.solidstategroup.radar.model.Sex;
import com.solidstategroup.radar.model.Status;
import com.solidstategroup.radar.util.TripleDes;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DemographicsDaoImpl extends BaseDaoImpl implements DemographicsDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemographicsDaoImpl.class);

    private static final String DATE_FORMAT = "dd.MM.yyyy";
    private static final String DATE_FORMAT_2 = "dd-MM-yyyy";

    private SimpleJdbcInsert demographicsInsert;

    private UtilityDao utilityDao;

    @Override
    public void setDataSource(DataSource dataSource) {
        // Call super
        super.setDataSource(dataSource);

        // Initialise a simple JDBC insert to be able to get the allocated ID
        demographicsInsert = new SimpleJdbcInsert(dataSource).withTableName("tbl_Demographics")
                .usingGeneratedKeyColumns("RADAR_NO")
                .usingColumns(
                        "RR_NO", "DATE_REG", "NHS_NO", "HOSP_NO", "UKT_NO", "CHI_NO", "SNAME", "SNAME_ALIAS",
                        "FNAME", "DOB", "AGE", "SEX", "ETHNIC_GP", "ADD1", "ADD2", "ADD3", "ADD4", "POSTCODE",
                        "POSTCODE_OLD", "CONSENT", "DATE_BAPN_REG", "CONS_NEPH", "RENAL_UNIT", "RENAL_UNIT_2", "STATUS"
                );
    }

    public void saveDemographics(final Demographics demographics) {
        // If we have an ID then update, otherwise insert new and set the ID
        if (demographics.hasValidId()) {
            jdbcTemplate.update(
                    "UPDATE tbl_Demographics SET RR_NO = ?, DATE_REG = ?, NHS_NO = ?, HOSP_NO = ?, UKT_NO = ?, " +
                            "CHI_NO = ?, SNAME = ?, SNAME_ALIAS = ?, FNAME = ?, DOB = ?, AGE = ?, SEX = ?, " +
                            "ETHNIC_GP = ?, ADD1 = ?, ADD2 = ?, ADD3 = ?, ADD4 = ?, POSTCODE = ?, POSTCODE_OLD = ?," +
                            "CONSENT = ?, DATE_BAPN_REG = ?, CONS_NEPH = ?, RENAL_UNIT = ?, RENAL_UNIT_2 = ?, " +
                            "STATUS = ?",
                    demographics.getRenalRegistryNumber(),
                    demographics.getDateRegistered(),
                    getEncryptedString(demographics.getNhsNumber()),
                    getEncryptedString(demographics.getHospitalNumber()),
                    demographics.getUkTransplantNumber(),
                    demographics.getChiNumber(),
                    getEncryptedString(demographics.getSurname()),
                    getEncryptedString(demographics.getSurnameAlias()),
                    getEncryptedString(demographics.getForename()),
                    getEncryptedString(new SimpleDateFormat(DATE_FORMAT).format(demographics.getDateOfBirth())),
                    demographics.getAge(),
                    demographics.getSex().getId(),
                    demographics.getEthnicity().getId(),
                    getEncryptedString(demographics.getAddress1()),
                    getEncryptedString(demographics.getAddress2()),
                    getEncryptedString(demographics.getAddress3()),
                    getEncryptedString(demographics.getAddress4()),
                    getEncryptedString(demographics.getPostcode()),
                    getEncryptedString(demographics.getPreviousPostcode()),
                    null, // Todo: demographics.getConsent(),
                    demographics.getDateRegistered(),
                    null, // Todo: Not sure what this should be,
                    demographics.getRenalUnit().getId(),
                    demographics.getRenalUnitAuthorised().getId(),
                    demographics.getStatus().getId()
            );
        } else {
            Number id = demographicsInsert.executeAndReturnKey(new HashMap<String, Object>() {
                {
                    put("RR_NO", demographics.getRenalRegistryNumber());
                    put("DATE_REG", demographics.getDateRegistered());
                    put("NHS_NO", demographics.getNhsNumber());
                    put("HOSP_NO", demographics.getHospitalNumber());
                    put("UKT_NO", demographics.getUkTransplantNumber());
                    put("CHI_NO", demographics.getChiNumber());
                    put("SNAME", getEncryptedString(demographics.getSurname()));
                    put("SNAME_ALIAS", getEncryptedString(demographics.getSurnameAlias()));
                    put("FNAME", getEncryptedString(demographics.getForename()));
                    put("DOB", demographics.getDateOfBirth());
                    put("AGE", demographics.getAge());
                    put("SEX", demographics.getSex() != null ? demographics.getSex().getId() : null);
                    put("ETHNIC_GP",
                            demographics.getEthnicity() != null ? demographics.getEthnicity().getCode() : null);
                    put("ADD1", demographics.getAddress1());
                    put("ADD2", demographics.getAddress2());
                    put("ADD3", demographics.getAddress3());
                    put("ADD4", demographics.getAddress4());
                    put("POSTCODE", demographics.getPostcode());
                    put("POSTCODE_OLD", demographics.getPreviousPostcode());
                    put("CONSENT", null); // Todo: Fix demographics.getC());
                    put("DATE_BAPN_REG", null); // Todo: Fix
                    put("CONS_NEPH", null); // Todo: Fix demographics.get());
                    put("RENAL_UNIT", demographics.getRenalUnit() != null ? demographics.getRenalUnit().getId() : null);
                    put("RENAL_UNIT_2", demographics.getRenalUnitAuthorised() != null ?
                            demographics.getRenalUnitAuthorised().getId() : null);
                    put("STATUS", demographics.getStatus() != null ? demographics.getStatus().getId() : null);
                }
            });
            demographics.setId(id.longValue());
        }
    }

    public Demographics getDemographicsByRadarNumber(long radarNumber) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM tbl_Demographics WHERE RADAR_NO = ?",
                    new Object[]{radarNumber}, new DemographicsRowMapper());
        } catch (EmptyResultDataAccessException e) {
            LOGGER.debug("No demographic record found for radar number {}", radarNumber);
            return null;
        }
    }

    public List<Demographics> getDemographicsByRenalUnit(Centre centre) {
        return jdbcTemplate.query("SELECT * FROM tbl_Demographics WHERE RENAL_UNIT = ?", new Object[]{centre.getId()},
                new DemographicsRowMapper());
    }

    public List<Demographics> getDemographics() {
        return jdbcTemplate.query("SELECT * FROM tbl_Demographics", new DemographicsRowMapper());
    }

    public Sex getSex(long id) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM tbl_Sex WHERE sID = ?", new Object[]{id},
                    new SexRowMapper());
        } catch (EmptyResultDataAccessException e) {
            LOGGER.debug("No sex found for ID {}", id);
            return null;
        }
    }

    public List<Sex> getSexes() {
        return jdbcTemplate.query("SELECT * FROM tbl_Sex", new SexRowMapper());
    }

    public Status getStatus(long id) {
        try {
            return jdbcTemplate
                    .queryForObject("SELECT * FROM tbl_Status WHERE sID = ?", new Object[]{id}, new StatusRowMapper());
        } catch (EmptyResultDataAccessException e) {
            LOGGER.debug("No status found for ID {}", id);
            return null;
        }
    }

    public List<Status> getStatuses() {
        return jdbcTemplate.query("SELECT * FROM tbl_Status", new StatusRowMapper());
    }

    private class DemographicsRowMapper implements RowMapper<Demographics> {
        public Demographics mapRow(ResultSet resultSet, int i) throws SQLException {
            // Construct object and set radar number
            Demographics demographics = new Demographics();
            demographics.setId(resultSet.getLong("RADAR_NO"));
            demographics.setDateRegistered(resultSet.getDate("DATE_REG"));

            // Renal registry number
            demographics.setRenalRegistryNumber(resultSet.getString("RR_NO"));

            // UK transplant number and chiNumber
            demographics.setUkTransplantNumber(resultSet.getString("UKT_NO"));
            demographics.setChiNumber(resultSet.getString("CHI_NO"));

            // These need to be decrypted from the database
            demographics.setNhsNumber(getDecryptedString(resultSet, "NHS_NO"));
            demographics.setHospitalNumber(getDecryptedString(resultSet, "HOSP_NO"));
            demographics.setSurname(getDecryptedString(resultSet, "SNAME"));
            demographics.setSurnameAlias(getDecryptedString(resultSet, "SNAME_ALIAS"));
            demographics.setForename(getDecryptedString(resultSet, "FNAME"));

            // Date needs to be decrypted to string, then parsed
            String dateOfBirthString = getDecryptedString(resultSet, "DOB");
            if (StringUtils.isNotBlank(dateOfBirthString)) {
                Date dateOfBirth = null;

                // It seems that the encrypted strings in the DB have different date formats, nice.
                for (String dateFormat : new String[]{DATE_FORMAT, DATE_FORMAT_2}) {
                    try {
                        dateOfBirth = new SimpleDateFormat(dateFormat).parse(dateOfBirthString);
                    } catch (ParseException e) {
                        LOGGER.debug("Could not parse date of birth {}", dateOfBirthString);
                    }
                }

                // If after trying those formats we don't have anything then log as error
                if (dateOfBirth != null) {
                    demographics.setDateOfBirth(dateOfBirth);
                } else {
                    LOGGER.error("Could not parse date of birth from any format for radar number {}",
                            demographics.getId());
                }
            }

            // Addresses, all encrypted too
            demographics.setAddress1(getDecryptedString(resultSet, "ADD1"));
            demographics.setAddress2(getDecryptedString(resultSet, "ADD2"));
            demographics.setAddress3(getDecryptedString(resultSet, "ADD3"));
            demographics.setAddress4(getDecryptedString(resultSet, "ADD4"));
            demographics.setPostcode(getDecryptedString(resultSet, "POSTCODE"));
            demographics.setPreviousPostcode(getDecryptedString(resultSet, "POSTCODE_OLD"));

            // Set sex
            demographics.setSex(getSex(resultSet.getLong("SEX")));

            // Try and get ethnicity
            String ethnicityCode = resultSet.getString("ETHNIC_GP");
            if (StringUtils.isNotBlank(ethnicityCode)) {
                demographics.setEthnicity(utilityDao.getEthnicityByCode(ethnicityCode));
            }

            demographics.setConsent(resultSet.getBoolean("CONSENT"));

            // Set the centre if we have an ID
            long renalUnitId = resultSet.getLong("RENAL_UNIT");
            if (renalUnitId > 0) {
                demographics.setRenalUnit(utilityDao.getCentre(renalUnitId));
            }

            // Set status
            long statusId = resultSet.getLong("STATUS");
            if (statusId > 0) {
                demographics.setStatus(getStatus(statusId));
            }

            // Fields to finish: ukTransplantNumber, chiNumber, consultant, renalUnitAuthorised

            return demographics;
        }
    }

    private class SexRowMapper implements RowMapper<Sex> {
        public Sex mapRow(ResultSet resultSet, int i) throws SQLException {
            Sex sex = new Sex();
            sex.setId(resultSet.getLong("sID"));
            sex.setType(resultSet.getString("sType"));
            return sex;
        }
    }

    private class StatusRowMapper implements RowMapper<Status> {
        public Status mapRow(ResultSet resultSet, int i) throws SQLException {
            // Contruct new status object
            Status status = new Status();
            status.setId(resultSet.getLong("sID"));
            status.setDescription(resultSet.getString("sDesc"));
            status.setAbbreviation(resultSet.getString("sAbbrev"));
            return status;
        }
    }

    private String getDecryptedString(ResultSet resultSet, String column) {
        try {
            // Catch the exception and log rather than throwing from entire row mapper
            return TripleDes.decrypt(resultSet.getBytes(column));
        } catch (Exception e) {
            LOGGER.error("Could not decrypt column data {} - {}", column, e.getMessage());
            return null;
        }
    }

    private byte[] getEncryptedString(String string) {
        // Todo: Implement
        return new byte[0];
    }

    public void setUtilityDao(UtilityDao utilityDao) {
        this.utilityDao = utilityDao;
    }

}
