package io.swagger.v3.plugins.gradle.internal;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import io.swagger.v3.plugins.gradle.convention.DocDeclaration;
import io.swagger.v3.plugins.gradle.convention.OpenApiExtension;

public class DefaultOpenApiExtension implements OpenApiExtension {

	private final Property<String> libraryVersion;

	private final NamedDomainObjectContainer<DocDeclaration> docs;

	@Inject
	public DefaultOpenApiExtension(Project project, ObjectFactory objectFactory) {
		this.libraryVersion = objectFactory.property(String.class);
		this.docs = project.container(DocDeclaration.class, name -> objectFactory.newInstance(DocDeclaration.class, name, project, objectFactory, project.getLayout()));
	}

	@Override
	public Property<String> getLibraryVersion() {
		return libraryVersion;
	}

	@Override
	public NamedDomainObjectContainer<DocDeclaration> getDocs() {
		return docs;
	}

	@Override
	public void docs(Action<? super NamedDomainObjectContainer<DocDeclaration>> action) {
		action.execute(docs);
	}
}
