/*
 * Copyright (c) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.samples.wildfly.restreader;

import java.util.Collection;
import java.util.List;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jberet.samples.wildfly.common.Movie;

@Path("/movies")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class MoviesResource {
    @Context
    private ServletContext servletContext;

    @GET
    public Movie[] getMovies(final @QueryParam("offset") int offset,
                             final @QueryParam("limit") int limit) {
        final List<Movie> resultList = getMoviesList0(offset, limit);
        System.out.printf("Returning Movie[]: %s elements%n%s%n", resultList.size(), resultList);
        return resultList.toArray(new Movie[resultList.size()]);
    }

    @Path("list")
    @GET
    public List<Movie> getMoviesList(final @QueryParam("offset") int offset,
                                     final @QueryParam("limit") int limit) {
        final List<Movie> resultList = getMoviesList0(offset, limit);
        System.out.printf("Returning List<Movie>: %s elements%n%s%n", resultList.size(), resultList);
        return resultList;
    }

    @Path("collection")
    @GET
    public Collection<Movie> getMoviesCollection(final @QueryParam("offset") int offset,
                                                 final @QueryParam("limit") int limit) {
        final List<Movie> resultList = getMoviesList0(offset, limit);
        System.out.printf("Returning Collection<Movie>: %s elements%n%s%n", resultList.size(), resultList);
        return resultList;
    }

    @SuppressWarnings("unchecked")
    private List<Movie> getMoviesList0(final int offset, final int limit) {
        final List<Movie> allMovies = (List<Movie>) servletContext.getAttribute(ServletContextListener1.moviesKey);
        return allMovies.subList(offset, Math.min(offset + limit, allMovies.size()));
    }
}
