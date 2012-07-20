package com.worthsoln.service.impl;

import com.worthsoln.patientview.Panel;
import com.worthsoln.patientview.model.Comment;
import com.worthsoln.repository.CommentDao;
import com.worthsoln.service.CommentManager;

import javax.inject.Inject;
import java.util.List;

/**
 *
 */
public class CommentManagerImpl implements CommentManager {

    @Inject
    private CommentDao commentDao;

    @Override
    public Comment get(Long id) {
        return commentDao.get(id);
    }

    @Override
    public List<Comment> get(String nhsno) {
        return commentDao.get(nhsno);
    }

    @Override
    public List<Comment> get(String nhsno, Panel currentPanel) {
        return commentDao.get(nhsno, currentPanel);
    }

    @Override
    public void save(Comment comment) {
        commentDao.save(comment);
    }
}