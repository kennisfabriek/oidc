/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.oidc.auth.internal.store;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.oidc.auth.internal.OIDCClientConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import org.slf4j.Logger;

/**
 * Helper to manager OpenID Connect profiles XClass and XObject.
 *
 * @version $Id: c68c46c340eb3dd4988644e71d45541e9c1f25eb $
 */
@Component(roles = OIDCUserStore.class)
@Singleton
public class OIDCUserStore
{
    @Inject
    private QueryManager queries;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

	@Inject
	private Logger logger;

	@Inject
	private OIDCClientConfiguration configuration;

	private String wikiRef;

	@Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    public boolean updateOIDCUser(XWikiDocument userDocument, String issuer, String subject)
    {
		this.wikiRef = configuration.getSubWikiId();


		this.logger.debug("Start updating OIDC user: {}" , userDocument.getDocumentReference().toString());


		XWikiContext xcontext = this.xcontextProvider.get();

		try {
			OIDCUser user = (OIDCUser) userDocument.getXObject(OIDCUser.CLASS_REFERENCE, true, xcontext);
			this.logger.debug("OIDC user: " + user.getDocumentReference().toString());

			boolean needUpdate = false;

			if (!StringUtils.equals(user.getIssuer(), issuer)) {
				user.setIssuer(issuer);
				needUpdate = true;
			}

			if (!StringUtils.equals(user.getSubject(), subject)) {
				user.setSubject(subject);
				needUpdate = true;
			}
			this.logger.debug("NeedsUpdate: " + needUpdate);


			return needUpdate;

		} catch (Exception e)
		{
			this.logger.debug("Exception op aanmaken class: " + e.getMessage());
		}

		return false;
    }

    public XWikiDocument searchDocument(String issuer, String subject) throws XWikiException, QueryException
    {
		this.wikiRef = configuration.getSubWikiId();

		Query query = this.queries.createQuery("from doc.object(" + OIDCUser.CLASS_FULLNAME
			+ ") as oidc where oidc.issuer = :issuer and oidc.subject = :subject", Query.XWQL);

		query.bindValue("issuer", issuer);
		query.bindValue("subject", subject);
		query.setWiki(this.wikiRef);

		List<String> documents = query.execute();

        if (documents.isEmpty()) {
			logger.debug("documents is empty");
            return null;
        }
		logger.debug("documents is NOT empty");

        // TODO: throw exception when there is several ?

        XWikiContext xcontext = this.xcontextProvider.get();

        DocumentReference userReference = this.resolver.resolve(documents.get(0));

		WikiReference myWikiRef = new WikiReference(this.wikiRef);

		userReference.setWikiReference(myWikiRef);

		DocumentReference useRef2 = userReference.setWikiReference(myWikiRef);

		xcontext.setWikiId(this.wikiRef);
		XWikiDocument userDocument = xcontext.getWiki().getDocument(useRef2, xcontext);

        if (userDocument.isNew()) {
			logger.debug("userDocument is NEW");
            return null;
        }

        return userDocument;
    }
}
