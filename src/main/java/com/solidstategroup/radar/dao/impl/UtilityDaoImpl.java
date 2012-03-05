package com.solidstategroup.radar.dao.impl;

import com.solidstategroup.radar.dao.UtilityDao;

import com.solidstategroup.radar.model.filter.ConsultantFilter;
import com.solidstategroup.radar.model.Consultant;
import com.solidstategroup.radar.model.Centre;
import com.solidstategroup.radar.model.Country;
import com.solidstategroup.radar.model.Ethnicity;
import com.solidstategroup.radar.model.Relative;
import com.solidstategroup.radar.model.DiagnosisCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.apache.commons.lang.StringUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class UtilityDaoImpl extends BaseDaoImpl implements UtilityDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtilityDaoImpl.class);

    private SimpleJdbcInsert consultantsInsert;

    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);

        consultantsInsert = new SimpleJdbcInsert(dataSource).withTableName("tbl_Consultants")
                .usingGeneratedKeyColumns("cID")
                .usingColumns("cSNAME", "cFNAME", "cCentre");
    }

    public Centre getCentre(long id) {
        return jdbcTemplate
                .queryForObject("SELECT * FROM tbl_Centres WHERE cID = ?", new Object[]{id}, new CentreRowMapper());
    }

    public List<Centre> getCentres() {
        return jdbcTemplate.query("SELECT * FROM tbl_Centres", new CentreRowMapper());
    }

    public Consultant getConsultant(long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM tbl_Consultants WHERE cID = ?", new Object[]{id},
                new ConsultantRowMapper());
    }

    public List<Consultant> getConsultants(ConsultantFilter filter, int page, int numberPerPage) {
        if (filter == null) {
            filter = new ConsultantFilter();
        }

        List<String> sqlQueries = new ArrayList<String>();
        List<Object> params = new ArrayList<Object>();

        // normal sql query without any filter options
        sqlQueries.add("SELECT " +
                "   tbl_Consultants.*, " +
                "   tbl_Centres.cName AS cName " +
                "FROM " +
                "   tbl_Consultants " +
                "INNER JOIN " +
                "   tbl_Centres " +
                "ON " +
                "   tbl_Consultants.cCentre = tbl_Centres.cID");

        // if there are search queries then build the where
        if (filter.hasSearchCriteria()) {
            sqlQueries.add(buildWhereQuery(filter.getSearchFields(), true, params));
        }

        // if the filter has a sort then order by it
        if (filter.hasSortFilter()) {
            sqlQueries.add(buildOrderQuery(filter.getSortField(), filter.isReverse()));
        }

        // if a range has been set limit the results
        sqlQueries.add(buildLimitQuery(page, numberPerPage, params));

        // combine the statement and return result
        return jdbcTemplate.query(StringUtils.join(sqlQueries.toArray(), " "), params.toArray(),
                new ConsultantRowMapper());
    }

    public List<Consultant> getConsultantsByCentre(Centre centre) {
        return jdbcTemplate.query("SELECT * FROM tbl_Consultants WHERE cCentre = ?", new Object[]{centre.getId()},
                new ConsultantRowMapper());

    }

    public void saveConsultant(final Consultant consultant) throws Exception {
        Map<String, Object> consultantMap = new HashMap<String, Object>() {
            {
                put("cSNAME", consultant.getSurname());
                put("cFNAME", consultant.getForename());
                put("cCentre", consultant.getCentre().getId());
                put("cID", consultant.getId());
            }
        };

        if (consultant.hasValidId()) {
            String updateSql = buildUpdateQuery("tbl_Consultants", "cID", consultantMap);
            namedParameterJdbcTemplate.update(updateSql, consultantMap);
        } else {
            Number id = consultantsInsert.executeAndReturnKey(consultantMap);
            consultant.setId(id.longValue());
        }
    }

    public void deleteConsultant(Consultant consultant) throws Exception {
        Map<String, Object> consultantMap = new HashMap<String, Object>();
        consultantMap.put("cID", consultant.getId());
        namedParameterJdbcTemplate.update("DELETE FROM tbl_Consultants WHERE cID = :cID;", consultantMap);
    }

    public Country getCountry(long id) {
        try {
            return jdbcTemplate
                    .queryForObject("SELECT * FROM tbl_Country WHERE cID = ?", new Long[]{id}, new CountryRowMapper());
        } catch (EmptyResultDataAccessException e) {
            LOGGER.debug("Could not get country with id {}", id);
            return null;
        }

    }

    public List<Country> getCountries() {
        return jdbcTemplate.query("SELECT * FROM tbl_Country", new CountryRowMapper());
    }

    public Ethnicity getEthnicityByCode(String ethnicityCode) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM tbl_Ethnicity WHERE eCode = ?",
                    new Object[]{ethnicityCode}, new EthnicityRowMapper());
        } catch (EmptyResultDataAccessException e) {
            LOGGER.debug("Could not get ethnicity with code {}", ethnicityCode);
            return null;
        }
    }

    public List<Ethnicity> getEthnicities() {
        return jdbcTemplate.query("SELECT * FROM tbl_Ethnicity", new EthnicityRowMapper());
    }

    public Relative getRelative(long id) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM tbl_Relative WHERE rID = ?", new Object[]{id},
                    new RelativeRowMapper());
        } catch (EmptyResultDataAccessException e) {
            LOGGER.debug("Could not get relative with ID {}", id);
            return null;
        }
    }

    public List<Relative> getRelatives() {
        return jdbcTemplate.query("SELECT * FROM tbl_Relative", new RelativeRowMapper());
    }

    public Map<Long, Integer> getPatientCountPerUnitByDiagnosisCode(DiagnosisCode diagnosisCode) {
        List<PatientCountItem> patientCountList = jdbcTemplate.query("SELECT COUNT(*) as \"count\", renal_unit " +
                "FROM tbl_demographics demographics INNER JOIN tbl_diagnosis diagnosis ON demographics.radar_no = " +
                "diagnosis.radar_no WHERE diag = ? " +
                "GROUP BY renal_unit;", new Object[]{diagnosisCode.getId()},
                new PatientCountByUnitRowMapper());

        Map<Long, Integer> patientCountMap = new HashMap<Long, Integer>();

        for (PatientCountItem item : patientCountList) {
            patientCountMap.put(item.getHospitalId(), item.getCount());
        }
        return patientCountMap;
    }

    public int getPatientCountByUnit(Centre centre) {
        try {
            return jdbcTemplate.queryForInt("SELECT COUNT(*) " +
                    "FROM tbl_demographics " +
                    "WHERE renal_unit = ? " +
                    "GROUP BY renal_unit;", new Object[]{centre.getId()});
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private class CentreRowMapper implements RowMapper<Centre> {
        public Centre mapRow(ResultSet resultSet, int i) throws SQLException {
            // Create a centre and set the fields from the resultset
            Centre centre = new Centre();
            centre.setId(resultSet.getLong("cID"));
            centre.setName(resultSet.getString("cName"));
            centre.setAbbreviation(resultSet.getString("cAbbrev"));
            // Set country from our DAO
            centre.setCountry(getCountry(resultSet.getLong("cCountry")));
            return centre;
        }
    }

    private class CountryRowMapper implements RowMapper<Country> {
        public Country mapRow(ResultSet resultSet, int i) throws SQLException {
            // Create a country and set the fields from our resultset
            Country country = new Country();
            country.setId(resultSet.getLong("cID"));
            country.setName(resultSet.getString("cName"));
            return country;
        }
    }

    private class EthnicityRowMapper implements RowMapper<Ethnicity> {
        public Ethnicity mapRow(ResultSet resultSet, int i) throws SQLException {
            // Construct ethnicity object and set fields
            Ethnicity ethnicity = new Ethnicity();
            ethnicity.setId(resultSet.getLong("eID"));
            ethnicity.setName(resultSet.getString("eName"));
            ethnicity.setCode(resultSet.getString("eCode"));
            return ethnicity;
        }
    }

    private class RelativeRowMapper implements RowMapper<Relative> {
        public Relative mapRow(ResultSet resultSet, int i) throws SQLException {
            // Construct a relative object and set all the fields
            Relative relative = new Relative();
            relative.setId(resultSet.getLong("rID"));
            relative.setName(resultSet.getString("RELATIVE"));
            return relative;
        }
    }

    private class ConsultantRowMapper implements RowMapper<Consultant> {
        public Consultant mapRow(ResultSet resultSet, int i) throws SQLException {
            // Construct a consultant object and set all the fields
            Consultant consultant = new Consultant();
            consultant.setId(resultSet.getLong("cID"));
            consultant.setSurname(resultSet.getString("cSNAME"));
            consultant.setForename(resultSet.getString("cFNAME"));

            // Centre could be null, in which case we get a 0 returned by getLong
            long centreId = resultSet.getLong("cCentre");
            if (centreId > 0) {
                consultant.setCentre(getCentre(centreId));
            }

            return consultant;
        }
    }

    private class PatientCountByUnitRowMapper implements RowMapper<PatientCountItem> {
        public PatientCountItem mapRow(ResultSet resultSet, int i) throws SQLException {

            return new PatientCountItem(resultSet.getLong("renal_unit"), resultSet.getInt("count"));
        }
    }

    private class PatientCountItem {
        long hospitalId;
        int count;

        private PatientCountItem(long hospitalId, int count) {
            this.hospitalId = hospitalId;
            this.count = count;
        }

        public long getHospitalId() {
            return hospitalId;
        }

        public void setHospitalId(long hospitalId) {
            this.hospitalId = hospitalId;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

}
