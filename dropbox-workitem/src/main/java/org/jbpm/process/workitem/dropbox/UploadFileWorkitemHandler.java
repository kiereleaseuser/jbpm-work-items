/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.process.workitem.dropbox;

import java.io.ByteArrayInputStream;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;
import org.jbpm.document.Document;
import org.jbpm.process.workitem.core.AbstractLogOrThrowWorkItemHandler;
import org.jbpm.process.workitem.core.util.Wid;
import org.jbpm.process.workitem.core.util.WidMavenDepends;
import org.jbpm.process.workitem.core.util.WidParameter;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Wid(widfile = "DropboxUploadFileDefinitions.wid", name = "DropboxUploadFile",
        displayName = "DropboxUploadFile",
        defaultHandler = "mvel: new org.jbpm.process.workitem.dropbox.UploadFileWorkitemHandler()",
        parameters = {
                @WidParameter(name = "Path"),
                @WidParameter(name = "Document")
        },
        mavenDepends = {
                @WidMavenDepends(group = "com.dropbox.core", artifact = "dropbox-core-sdk", version = "3.0.5")
        })
public class UploadFileWorkitemHandler extends AbstractLogOrThrowWorkItemHandler {

    private static final Logger logger = LoggerFactory.getLogger(UploadFileWorkitemHandler.class);

    private DropboxAuth auth;
    private DbxClientV2 client;
    private String clientIdentifier;
    private String accessToken;

    public UploadFileWorkitemHandler(String clientIdentifier,
                                     String accessToken) {
        this.clientIdentifier = clientIdentifier;
        this.accessToken = accessToken;
    }

    public void executeWorkItem(WorkItem workItem,
                                WorkItemManager workItemManager) {
        try {
            if (auth == null) {
                auth = new DropboxAuth();
            }

            client = auth.authorize(clientIdentifier,
                                    accessToken);

            String dropboxPath = (String) workItem.getParameter("Path");
            Document documentToUpload = (Document) workItem.getParameter("Document");

            ByteArrayInputStream docStream = new ByteArrayInputStream(documentToUpload.getContent());

            client.files().uploadBuilder(dropboxPath + "/" + documentToUpload.getName())
                    .withMode(WriteMode.ADD)
                    .withClientModified(documentToUpload.getLastModified())
                    .uploadAndFinish(docStream);

            workItemManager.completeWorkItem(workItem.getId(),
                                             null);
        } catch (Exception e) {
            logger.error("Unable to upload file: " + e.getMessage());
            handleException(e);
        }
    }

    public void abortWorkItem(WorkItem wi,
                              WorkItemManager wim) {
    }

    // for testing
    public void setAuth(DropboxAuth auth) {
        this.auth = auth;
    }
}
