package com.solidstategroup.radar.web.pages.admin;

import com.solidstategroup.radar.web.dataproviders.user.PatientUserDataProvider;
import com.solidstategroup.radar.web.behaviours.RadarBehaviourFactory;
import com.solidstategroup.radar.web.components.SortLink;
import com.solidstategroup.radar.service.UserManager;
import com.solidstategroup.radar.service.DemographicsManager;
import com.solidstategroup.radar.model.user.PatientUser;
import com.solidstategroup.radar.model.Demographics;
import com.solidstategroup.radar.model.filter.PatientUserFilter;
import com.solidstategroup.radar.util.TripleDes;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.Map;
import java.util.HashMap;

public class AdminPatientsPage extends AdminsBasePage {

    @SpringBean
    private UserManager userManager;
    @SpringBean
    private DemographicsManager demographicsManager;

    private static final int RESULTS_PER_PAGE = 10;

    public AdminPatientsPage() {
        final PatientUserDataProvider consultantsDataProvider = new PatientUserDataProvider(userManager);

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        feedback.setOutputMarkupPlaceholderTag(true);
        add(feedback);

        // TODO: need to hook these up
        add(new ExternalLink("exportPdf", ""));
        add(new ExternalLink("exportExcel", ""));

        final WebMarkupContainer patientsContainer = new WebMarkupContainer("patientsContainer");
        patientsContainer.setOutputMarkupId(true);
        add(patientsContainer);

        final DataView<PatientUser> patientList = new DataView<PatientUser>("patients",
                consultantsDataProvider) {
            @Override
            protected void populateItem(Item<PatientUser> item) {
                builtDataViewRow(item, feedback);
            }
        };
        patientList.setItemsPerPage(RESULTS_PER_PAGE);
        patientsContainer.add(patientList);

        // add paging element
        patientsContainer.add(new AjaxPagingNavigator("navigator", patientList));

        // add sort links to the table column headers
        for (Map.Entry<String, String> entry : getSortFields().entrySet()) {
            add(new SortLink(entry.getKey(), entry.getValue(), consultantsDataProvider) {
                @Override
                public void onClicked(AjaxRequestTarget ajaxRequestTarget) {
                    patientList.setCurrentPage(0);
                    ajaxRequestTarget.add(patientsContainer);
                }
            });
        }
    }

    /**
     * Build a row in the dataview from the object
     * @param item Item<PatientUser>
     * @param feedback
     */
    private void builtDataViewRow(final Item<PatientUser> item, final FeedbackPanel feedback) {
        final PatientUser patientUser = item.getModelObject();
        final Demographics demographics = demographicsManager.getDemographicsByRadarNumber(patientUser.getRadarNumber());

        item.add(new BookmarkablePageLink<AdminConsultantPage>("edit", AdminPatientsPage.class)); // TODO: make page and pass params

        item.add(new Label("radarNo", patientUser.getRadarNumber().toString()));
        item.add(new Label("forename", demographics.getForename()));
        item.add(new Label("surname", demographics.getSurname()));
        item.add(new Label("dob", patientUser.getDateOfBirth().toString()));

        String dateRegistered = "";
        if (patientUser.getDateRegistered() != null) {
            dateRegistered = patientUser.getDateRegistered().toString();
        }

        item.add(new Label("dateRegistered", dateRegistered));
        item.add(new Label("username", patientUser.getUsername()));

        String password;
        try {
            password = TripleDes.decrypt(patientUser.getPasswordHash());
        } catch (Exception e) {
            password = "";
        }

        item.add(new Label("password", password));

        AjaxLink deleteLink = new AjaxLink("deleteLink") {
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                try {
                    // todo: implement delete patient user
                    //userManager.deletePatientUser(patientUser);
                    setResponsePage(AdminPatientsPage.class);
                } catch (Exception e) {
                    error("Could not delete patient " + e);
                    ajaxRequestTarget.add(feedback);
                }
            }
        };
        deleteLink.add(RadarBehaviourFactory.getDeleteConfirmationBehaviour());
        item.add(deleteLink);

        item.add(new AjaxLink("emailLink") {
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
             // TODO:
            }
        });
    }

    /**
     * List of columns that can be used to sort the results - will return ID of el to be bound to and the field to sort
     * @return Map<String, PatientUserFilter.UserField>
     */
    private Map<String, String> getSortFields() {
        return new HashMap<String, String>() {
            {
                put("orderByRadarNo", PatientUserFilter.UserField.RADAR_NO.getDatabaseFieldName());
                put("orderByUsername", PatientUserFilter.UserField.USERNAME.getDatabaseFieldName());
                put("orderByDob", PatientUserFilter.UserField.DOB.getDatabaseFieldName());
                put("orderByDateRegistered", PatientUserFilter.UserField.REGISTRATION_DATE.getDatabaseFieldName());
            }
        };
    }
}
