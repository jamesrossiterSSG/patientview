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

package org.patientview.radar.web.panels;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.patientview.model.Patient;
import org.patientview.radar.model.Genetics;
import org.patientview.radar.service.alport.GeneticsManager;
import org.patientview.radar.web.RadarApplication;
import org.patientview.radar.web.components.ComponentHelper;
import org.patientview.radar.web.components.RadarComponentFactory;
import org.patientview.radar.web.components.RadarRequiredDateTextField;

import java.util.ArrayList;
import java.util.List;

public class NonAlportGeneticsPanel extends Panel {

    @SpringBean
    private GeneticsManager geneticsManager;

    public NonAlportGeneticsPanel(final String id, final Patient patient) {
        super(id);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        Genetics genetics = null;

        if (patient.hasValidId()) {
            genetics = geneticsManager.get(patient.getRadarNo());
        }

        if (genetics == null) {
            genetics = new Genetics();
            genetics.setRadarNo(patient.getRadarNo());
        }

        // main model for this tab
        final IModel<Genetics> model = new Model<Genetics>(genetics);

        // components to update on ajax refresh
        final List<Component> componentsToUpdateList = new ArrayList<Component>();

        // general feedback for messages that are not to do with a certain component in the form
        final FeedbackPanel formFeedback = new FeedbackPanel("formFeedbackPanel");
        formFeedback.setOutputMarkupId(true);
        formFeedback.setOutputMarkupPlaceholderTag(true);
        componentsToUpdateList.add(formFeedback);

        Form<Genetics> form = new Form<Genetics>("form", new CompoundPropertyModel<Genetics>(model)) {
            @Override
            protected void onSubmit() {
                Genetics genetics = getModelObject();

                if (genetics.getLabWhereTestWasDone() != null && genetics.getLabWhereTestWasDone().length() > 150) {
                    error("Laboratory where sent/done is limited to 150 characters.");
                }

                // requirement is to limit to 20 lines of text -> 130 chars per line ~ 2500 chars
                if (genetics.getWhatResultsShowed() != null && genetics.getWhatResultsShowed().length() > 2500) {
                    error("Laboratory where sent/done is limited to 2500 characters.");
                }

                if (genetics.getTestsDone() == null) {
                    error("Please select if a sample been sent for Genetic analysis");
                }

                if (Genetics.TestsDone.YES_IN_THIS_PATIENT.equals(genetics.getTestsDone())) {
                    if (genetics.getDateSent() == null) {
                        error("Please select Date Sent if a sample been sent.");
                    }
                }

                if (!hasError()) {
                    genetics.setRadarNo(patient.getRadarNo());
                    geneticsManager.save(genetics);
                }
            }
        };

        add(form);

        // have to set the generic feedback panel to only pick up msgs for them form
        ComponentFeedbackMessageFilter filter = new ComponentFeedbackMessageFilter(form);
        formFeedback.setFilter(filter);
        form.add(formFeedback);

        // add the patient detail bar to the tab
        PatientDetailPanel patientDetail = new PatientDetailPanel("patientDetail", patient, "Genetics");
        patientDetail.setOutputMarkupId(true);
        form.add(patientDetail);
        componentsToUpdateList.add(patientDetail);

        // Date picker
        final DateTextField dateSent = new RadarRequiredDateTextField("dateSent",
                form, componentsToUpdateList);
        componentsToUpdateList.add(dateSent);
        MarkupContainer dateSentLabel = new WebMarkupContainer("dateSentLabel") {
            @Override
            public boolean isVisible() {
                Genetics genetics = model.getObject();
                if (genetics.getTestsDone() == null || genetics.getTestsDone().equals(Genetics.TestsDone.NO)) {
                    return false;
                } else {
                    return true;
                }
            }
        };
        dateSentLabel.setOutputMarkupId(true);
        dateSentLabel.add(dateSent);
        dateSentLabel.setOutputMarkupPlaceholderTag(true);

        form.add(dateSentLabel);
        componentsToUpdateList.add(dateSentLabel);

//        if (genetics.getTestsDone() == null || genetics.getTestsDone().equals(Genetics.TestsDone.NO)){
//            dateSent.setVisible(false);
//            dateSentLabel.setVisible(false);
//        }

        RadioGroup<Genetics.TestsDone> testsDoneRadioGroup = new RadioGroup<Genetics.TestsDone>("testsDone");
        form.add(testsDoneRadioGroup);

        Radio testsDoneNo = new Radio<Genetics.TestsDone>("testsDoneNo",
                new Model<Genetics.TestsDone>(Genetics.TestsDone.NO));
        testsDoneRadioGroup.add(testsDoneNo);
        testsDoneNo.add(new AjaxEventBehavior("onchange") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                model.getObject().setTestsDone(Genetics.TestsDone.NO);
                target.add(componentsToUpdateList.toArray(new Component[componentsToUpdateList.size()]));
            }
        });
        Radio testsDoneYes = new Radio<Genetics.TestsDone>("testsDoneYes",
                new Model<Genetics.TestsDone>(Genetics.TestsDone.YES_IN_THIS_PATIENT));
        testsDoneRadioGroup.add(testsDoneYes);
        testsDoneYes.add(new AjaxEventBehavior("onchange") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                model.getObject().setTestsDone(Genetics.TestsDone.YES_IN_THIS_PATIENT);
                target.add(componentsToUpdateList.toArray(new Component[componentsToUpdateList.size()]));
            }
        });

        form.add(new TextArea<String>("labWhereTestWasDone"));
        form.add(new TextField<String>("referenceNumber"));
        form.add(new TextArea<String>("whatResultsShowed"));

        Label successMessageTop = RadarComponentFactory.getSuccessMessageLabel("successMessageTop", form,
                componentsToUpdateList);
        Label errorMessageTop = RadarComponentFactory.getErrorMessageLabel("errorMessageTop", form,
                componentsToUpdateList);

        Label successMessageBottom = RadarComponentFactory.getSuccessMessageLabel("successMessageBottom", form,
                componentsToUpdateList);
        Label errorMessageBottom = RadarComponentFactory.getErrorMessageLabel("errorMessageBottom",
                form, componentsToUpdateList);

        form.add(new AjaxSubmitLink("saveTop") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                ComponentHelper.updateComponentsIfParentIsVisible(target, componentsToUpdateList);
                target.appendJavaScript(RadarApplication.FORM_IS_DIRTY_FALSE_SCRIPT);
                target.add(formFeedback);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                ComponentHelper.updateComponentsIfParentIsVisible(target, componentsToUpdateList);
                target.add(formFeedback);
            }
        });

        form.add(new AjaxSubmitLink("saveBottom") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                ComponentHelper.updateComponentsIfParentIsVisible(target, componentsToUpdateList);
                target.appendJavaScript(RadarApplication.FORM_IS_DIRTY_FALSE_SCRIPT);
                target.add(formFeedback);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                ComponentHelper.updateComponentsIfParentIsVisible(target, componentsToUpdateList);
                target.add(formFeedback);
            }
        });
    }
}
