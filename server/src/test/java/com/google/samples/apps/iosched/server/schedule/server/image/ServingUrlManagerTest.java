/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.server.schedule.server.image;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for ServingUrlManager.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyFactory.class, Key.class, Query.class, ServingUrlManager.class})
public class ServingUrlManagerTest {

  private static final GcsFilename GCS_FILENAME = new GcsFilename("fakebucket", "fakepath");
  private static final String GCS_FULLPATH = "/gs/fakebucket/fakepath";
  private static final String SOURCE_URL = "http://fake/source_url";
  private static final String SERVING_URL = "http://fake/serving_url";

  @Mock
  private DatastoreService mockDatastoreService;

  @Mock
  private ImagesService mockImagesService;

  @Before
  public void setUp() throws Exception {
    mockStatic(KeyFactory.class);
    when(KeyFactory.createKey(anyString(), anyString())).thenAnswer(
        new Answer<Key>() {
          @Override
          public Key answer(InvocationOnMock invocation) throws Throwable {
            Key key = mock(Key.class);
            when(key.getKind()).thenReturn((String) invocation.getArguments()[0]);
            when(key.getName()).thenReturn((String) invocation.getArguments()[1]);
            return key;
          }
        });

    whenNew(Query.class).withArguments(anyString()).thenAnswer(
        new Answer<Query>() {
          @Override
          public Query answer(InvocationOnMock invocation) throws Throwable {
            final Query query = mock(Query.class);
            when(query.getKind()).thenReturn((String) invocation.getArguments()[0]);
            when(query.setFilter(any(Filter.class))).thenAnswer(
                new Answer<Query>() {
                  @Override
                  public Query answer(InvocationOnMock invocation) throws Throwable {
                    when(query.getFilter()).thenReturn((Filter) invocation.getArguments()[0]);
                    return query;
                  }
                }
            );
            return query;
          }
        });

    ServingUrlManager.INSTANCE.datastore = mockDatastoreService;
    ServingUrlManager.INSTANCE.imagesService = mockImagesService;

    when(mockImagesService.getServingUrl(any(ServingUrlOptions.class)))
        .thenReturn(SERVING_URL);
  }

  @Test
  public void testCreateServingUrl() throws Exception {
    when(mockDatastoreService.get(any(Key.class)))
        .thenThrow(new EntityNotFoundException(null));

    assertEquals(SERVING_URL,
        ServingUrlManager.INSTANCE.createServingUrl(GCS_FILENAME, Optional.<String>absent()));

    verify(mockImagesService).getServingUrl(eq(ServingUrlOptions.Builder
        .withGoogleStorageFileName(GCS_FULLPATH).secureUrl(true)));

    ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(mockDatastoreService).put(entityCaptor.capture());

    Entity entity = entityCaptor.getValue();
    assertEquals(ServingUrlManager.ENTITY_KIND, entity.getKey().getKind());
    assertEquals(GCS_FULLPATH, entity.getKey().getName());
    assertEquals("", entity.getProperty(ServingUrlManager.SOURCE_URL_PROPERTY));
    assertEquals(SERVING_URL, entity.getProperty(ServingUrlManager.SERVING_URL_PROPERTY));
  }

  @Test
  public void testCreateServingUrl_withSourceUrl() throws Exception {
    when(mockDatastoreService.get(any(Key.class)))
        .thenThrow(new EntityNotFoundException(null));

    assertEquals(SERVING_URL,
        ServingUrlManager.INSTANCE.createServingUrl(GCS_FILENAME, Optional.of(SOURCE_URL)));

    verify(mockImagesService).getServingUrl(eq(ServingUrlOptions.Builder
        .withGoogleStorageFileName(GCS_FULLPATH).secureUrl(true)));

    ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(mockDatastoreService).put(entityCaptor.capture());

    Entity entity = entityCaptor.getValue();
    assertEquals(ServingUrlManager.ENTITY_KIND, entity.getKey().getKind());
    assertEquals(GCS_FULLPATH, entity.getKey().getName());
    assertEquals(SOURCE_URL, entity.getProperty(ServingUrlManager.SOURCE_URL_PROPERTY));
    assertEquals(SERVING_URL, entity.getProperty(ServingUrlManager.SERVING_URL_PROPERTY));
  }

  @Test
  public void testCreateServingUrl_servingUrlAlreadyExists() throws Exception {
    when(mockDatastoreService.get(any(Key.class))).thenAnswer(
        new Answer<Entity>() {
          @Override
          public Entity answer(InvocationOnMock invocation) throws Throwable {
            Entity key = new Entity((Key) invocation.getArguments()[0]);
            key.setProperty(ServingUrlManager.SERVING_URL_PROPERTY, SERVING_URL);
            return key;
          }
        });

    assertEquals(SERVING_URL,
        ServingUrlManager.INSTANCE.createServingUrl(GCS_FILENAME, Optional.<String>absent()));

    verifyZeroInteractions(mockImagesService);
    verify(mockDatastoreService, never()).put(any(Entity.class));
  }

  @Test
  public void testGetServingUrl_withGcsFilename() throws Exception {
    when(mockDatastoreService.get(any(Key.class))).thenAnswer(
        new Answer<Entity>() {
          @Override
          public Entity answer(InvocationOnMock invocation) throws Throwable {
            Entity entity = new Entity((Key) invocation.getArguments()[0]);
            entity.setProperty(ServingUrlManager.SERVING_URL_PROPERTY, SERVING_URL);
            return entity;
          }
        });

    assertEquals(SERVING_URL, ServingUrlManager.INSTANCE.getServingUrl(GCS_FILENAME));

    ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);
    verify(mockDatastoreService).get(keyCaptor.capture());

    Key key = keyCaptor.getValue();
    assertEquals(ServingUrlManager.ENTITY_KIND, key.getKind());
    assertEquals(GCS_FULLPATH, key.getName());
  }

  @Test
  public void testGetServingUrl_withSourceUrl() throws Exception {
    PreparedQuery mockPreparedQuery = mock(PreparedQuery.class);
    when(mockDatastoreService.prepare(any(Query.class))).thenReturn(mockPreparedQuery);
    when(mockPreparedQuery.asList(any(FetchOptions.class))).thenAnswer(
        new Answer<List<Entity>>() {
          @Override
          public List<Entity> answer(InvocationOnMock invocation) throws Throwable {
            Key key = mock(Key.class);
            when(key.getKind()).thenReturn(ServingUrlManager.ENTITY_KIND);
            when(key.getName()).thenReturn(GCS_FULLPATH);
            Entity entity = new Entity(key);
            entity.setProperty(ServingUrlManager.SERVING_URL_PROPERTY, SERVING_URL);
            return new ArrayList<>(Arrays.asList(entity));
          }
        });

    assertEquals(SERVING_URL, ServingUrlManager.INSTANCE.getServingUrl(SOURCE_URL));

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    verify(mockDatastoreService).prepare(queryCaptor.capture());

    Query query = queryCaptor.getValue();
    assertEquals(ServingUrlManager.ENTITY_KIND, query.getKind());
    assertEquals(new FilterPredicate(ServingUrlManager.SOURCE_URL_PROPERTY, FilterOperator.EQUAL,
        SOURCE_URL), query.getFilter());
  }
}
