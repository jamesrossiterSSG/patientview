package com.worthsoln.patientview.model;

import com.worthsoln.patientview.model.enums.GroupEnum;
import com.worthsoln.patientview.model.enums.SendEmailEnum;

import javax.persistence.JoinColumn;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import java.util.Date;

@Entity
public class Job extends BaseModel {

    @ManyToOne(optional = false)
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToOne(optional = false)
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SendEmailEnum status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupEnum groupEnum;

    @Column(nullable = false)
    private Date created;

    @Column(nullable = true)
    private Date started;

    @Column(nullable = true)
    private Date finished;

    @Min(value = 0)
    private long errorCount;

    @Transient
    @Column(nullable = false, columnDefinition = "TEXT")
    private StringBuilder reports;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Specialty getSpecialty() {
        return specialty;
    }

    public void setSpecialty(Specialty specialty) {
        this.specialty = specialty;
    }

    public SendEmailEnum getStatus() {
        return status;
    }

    public void setStatus(SendEmailEnum status) {
        this.status = status;
    }

    public GroupEnum getGroupEnum() {
        return groupEnum;
    }

    public void setGroupEnum(GroupEnum groupEnum) {
        this.groupEnum = groupEnum;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getFinished() {
        return finished;
    }

    public void setFinished(Date finished) {
        this.finished = finished;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public StringBuilder getReports() {
        return reports;
    }

    public void setReports(StringBuilder reports) {
        this.reports = reports;
    }
}
