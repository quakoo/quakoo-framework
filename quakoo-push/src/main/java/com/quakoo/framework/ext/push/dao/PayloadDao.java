package com.quakoo.framework.ext.push.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.push.model.Payload;


public interface PayloadDao {

    public List<Long> getPayloadIds(int size) throws DataAccessException;

	public Payload insert(Payload payload) throws DataAccessException;

    public List<Payload> insert(List<Payload> payloads) throws DataAccessException;

	public Payload load(long id) throws DataAccessException;

	public List<Payload> load(List<Long> ids) throws DataAccessException;

}
