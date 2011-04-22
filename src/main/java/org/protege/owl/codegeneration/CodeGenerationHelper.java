package org.protege.owl.codegeneration;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class CodeGenerationHelper {
    private OWLOntology owlOntology;
    private OWLNamedIndividual owlIndividual;
    
    private OWLDataFactory owlDataFactory;
    private OWLOntologyManager manager;
    
    
    public CodeGenerationHelper(OWLOntology owlOntology, OWLNamedIndividual individual) {
        this.owlOntology = owlOntology;
        owlIndividual = individual;
        
        manager = owlOntology.getOWLOntologyManager();
        owlDataFactory = manager.getOWLDataFactory();
    }
    
    public OWLOntology getOwlOntology() {
        return owlOntology;
    }
    
    public OWLNamedIndividual getOwlIndividual() {
		return owlIndividual;
	}
    
    public <X> Collection<X> getPropertyValues(OWLNamedIndividual i, OWLObjectProperty p, Class<X> c) {
    	try {
    		Constructor<X> constructor = c.getConstructor(OWLOntology.class, IRI.class);
    		Set<X> results = new HashSet<X>();
    		for (OWLOntology imported : owlOntology.getImportsClosure()) {
    			for (OWLIndividual j : i.getObjectPropertyValues(p, imported)) {
    				if (!j.isAnonymous()) {
    					results.add(constructor.newInstance(owlOntology, j.asOWLNamedIndividual().getIRI()));
    				}
    			}
    		}
    		return results;
    	}
    	catch (Exception e) {
    		throw new CodeGenerationRuntimeException(e);
    	}
    }
 
    public void addPropertyValue(OWLNamedIndividual i, OWLObjectProperty p, WrappedIndividual j) {
    	OWLAxiom axiom = owlDataFactory.getOWLObjectPropertyAssertionAxiom(p, i, j.getOwlIndividual());
    	manager.addAxiom(owlOntology, axiom);
    }
    
    public void removePropertyValue(OWLNamedIndividual i, OWLObjectProperty p, WrappedIndividual j) {
    	OWLAxiom axiom = owlDataFactory.getOWLObjectPropertyAssertionAxiom(p, i, j.getOwlIndividual());
    	for (OWLOntology imported : owlOntology.getImportsClosure()) {
        	manager.removeAxiom(imported, axiom);
    	}
    }
    
    public <X> Collection<X> getPropertyValues(OWLNamedIndividual i, OWLDataProperty p, Class<X> c) {
    	Set<X> results = new HashSet<X>();
    	for (OWLOntology imported : owlOntology.getImportsClosure()) {
    		for (OWLLiteral l : i.getDataPropertyValues(p, imported)) {
    			results.add(c.cast(getObjectFromLiteral(l)));
    		}
    	}
    	return results;
    }
    
    public void addPropertyValue(OWLNamedIndividual i, OWLDataProperty p, Object o) {
    	OWLLiteral literal = getLiteralFromObject(owlDataFactory, o);
    	if (literal != null) {
    		OWLAxiom axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(p, i, literal);
    		manager.addAxiom(owlOntology, axiom);
    	}
    	else {
    		throw new CodeGenerationRuntimeException("Invalid type for property value object " + o);
    	}
    }

    public void removePropertyValue(OWLNamedIndividual i, OWLDataProperty p, Object o) {
    	OWLLiteral literal = getLiteralFromObject(owlDataFactory, o);
    	if (literal != null) {
    		OWLAxiom axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(p, i, literal);
    		manager.removeAxiom(owlOntology, axiom);
    	}
    	else {
    		throw new CodeGenerationRuntimeException("Invalid type for property value object " + o);
    	}
    }
    
    public static Object getObjectFromLiteral(OWLLiteral literal) {
    	Object o = null;
    	for (HandledDatatypes handled : HandledDatatypes.values()) {
    		if (handled.isMatch(literal.getDatatype())) {
    			o = handled.getObject(literal);
    			break;
    		}
    	}
    	if (o == null) {
    		o = literal;
    	}
    	return o;
    }
    
    public static OWLLiteral getLiteralFromObject(OWLDataFactory owlDataFactory, Object o) {
    	OWLLiteral literal = null;
    	if (o instanceof OWLLiteral) {
    		literal = (OWLLiteral) o;
    	}
    	else {
    		for (HandledDatatypes handled : HandledDatatypes.values()) {
    			literal = handled.getLiteral(owlDataFactory, o);
    			if (literal != null) {
    				break;
    			}
    		}
    	}
    	return literal;
    }
    
}
