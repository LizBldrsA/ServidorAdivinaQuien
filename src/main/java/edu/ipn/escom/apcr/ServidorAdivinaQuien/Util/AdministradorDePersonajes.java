package edu.ipn.escom.apcr.ServidorAdivinaQuien.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import edu.ipn.escom.apcr.ServidorAdivinaQuien.Beans.Personaje;

public class AdministradorDePersonajes {

	public static final String RUTA_ARCHIVOS = "/home/millocorona/Desarrollo/Java/ServidorAdivinaQuien/run";
	public static final String ARCHIVO_XML_PERSONAJES = "DATOS_PERSONAJES.xml";
	
	public void iniciarAdministrador() {
		System.out.println("***********************************************");
		System.out.println("* Administrador de personajes - Adivina quien *");
		System.out.println("***********************************************");
		boolean continuar = true;
		while(continuar) {
			System.out.println();
			System.out.println("¿Que desea hacer?");
			System.out.println();
			System.out.println("\t 1.- Agregar personaje");
			System.out.println("\t 2.- Modificar personaje");
			System.out.println("\t 3.- Eliminar personaje");
			System.out.println("\t 4.- Salir");
			boolean opcionValida = false;
			while (!opcionValida) {
				int opcion = new Scanner(System.in).nextInt();
				if(opcion == 1) {
					try {
						Personaje personaje = new Personaje();
						System.out.println("Ingrese el nombre del personaje");
						personaje.setNombre(new Scanner(System.in).nextLine());
						System.out.println("Ingrese las caracteristicas del personaje (separadas por comas)");
						personaje.setCaracteristicas(new LinkedList<String>(Arrays.asList(new Scanner(System.in).nextLine().split(","))));
						boolean rutaValida = false;
						while(!rutaValida) {
							System.out.println("Ingrese la ruta de la imagen del personaje");
							String rutaImagen = new Scanner(System.in).nextLine();
							File archivoImagen = new File(rutaImagen);
							if(archivoImagen.exists() && archivoImagen.isFile()) {
								//Convertimos la imagen a base 64
								FileInputStream fileInputStreamReader = new FileInputStream(archivoImagen);
				                byte[] bytesImagen = new byte[(int) archivoImagen.length()];
				                fileInputStreamReader.read(bytesImagen);
								String imagenBase64 = new String(Base64.getEncoder().encode(bytesImagen),StandardCharsets.UTF_8);
								personaje.setDatosImagenBase64(imagenBase64);
								rutaValida = true;
							}
						}
						agregarPersonaje(personaje);
						System.out.println("Personaje agregado");
					} catch (JDOMException | IOException e) {
						e.printStackTrace();
					}
					opcionValida = true;
				}else if(opcion == 2){
					System.out.println("Esta opcion no esta implementada aun, se implementara en el futuro");
					opcionValida = true;
				}else if (opcion == 3) {
					System.out.println();
					try {
						LinkedList<Personaje> personajes = obtenerPersonajesDeArchivo();
						if(personajes.size()>0) {
							for(int i = 0;i<personajes.size();i++) {
								System.out.println((i+1)+") "+personajes.get(i).getNombre());
								System.out.println();
							}
							
							int numeroPersonajeAEliminar = -1;
							boolean numeroPersonajeValido = false;
							while(!numeroPersonajeValido) {
								numeroPersonajeAEliminar = new Scanner(System.in).nextInt()-1;
								if(numeroPersonajeAEliminar<personajes.size()) {
									numeroPersonajeValido = true;
								}
							}
							eliminarPersonaje(personajes.get(numeroPersonajeAEliminar));
						}else {
							System.out.println("No hay personajes agregados");
						}
						
					} catch (JDOMException | IOException e) {
						e.printStackTrace();
					}					
					opcionValida = true;
				}else if (opcion == 4){
					System.out.println("Hasta luego");
					continuar = false;
					opcionValida = true;
				}else {
					System.out.println("Ingrese una opcion valida");
				}
			}	
		}
	}
		
	private void agregarPersonaje(Personaje personaje) throws JDOMException, IOException {
		File archivoXMLPersonajes = new File(RUTA_ARCHIVOS+File.separator+ARCHIVO_XML_PERSONAJES);
		Document documentoXMLPersonajes;
		if(archivoXMLPersonajes.exists()) {
			documentoXMLPersonajes = (Document) new SAXBuilder().build(archivoXMLPersonajes);
		}else {
			archivoXMLPersonajes.createNewFile();
			Element elementoPersonajes = new Element("personajes");
			documentoXMLPersonajes  = new Document(elementoPersonajes);
			documentoXMLPersonajes.setRootElement(elementoPersonajes);
		}
		
		Element nodoRaizXML = documentoXMLPersonajes.getRootElement();
		Element elementoPersonaje = new Element("personaje");
		elementoPersonaje.setAttribute("nombre",personaje.getNombre());
		elementoPersonaje.setAttribute("datosImagenBase64",personaje.getDatosImagenBase64());
		for(String caracteristica:personaje.getCaracteristicas()) {
			Element elementoCaracteristica = new Element("caracteristica");
			elementoCaracteristica.setText(caracteristica);
			elementoPersonaje.addContent(elementoCaracteristica);
		}
		nodoRaizXML.addContent(elementoPersonaje);
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(documentoXMLPersonajes, new FileWriter(archivoXMLPersonajes));
	}
	
	private void modificarPersonaje(Personaje personaje) {
		throw new UnsupportedOperationException();
	}
	
	private void eliminarPersonaje(Personaje personaje) throws JDOMException, IOException {
		File archivoXMLPersonajes = new File(RUTA_ARCHIVOS+File.separator+ARCHIVO_XML_PERSONAJES);
		Document documentoXMLPersonajes = (Document) new SAXBuilder().build(archivoXMLPersonajes);
		Element nodoRaizXML = documentoXMLPersonajes.getRootElement();
		List<Element> listaElementosPersonajes = (List<Element>) nodoRaizXML.getChildren("personaje");
		for (Element elementoPersonaje:listaElementosPersonajes) {
			if(elementoPersonaje.getAttribute("nombre").getValue().trim().equals(personaje.getNombre())) {
				elementoPersonaje.getParent().removeContent(elementoPersonaje);
				break;
			}
		}
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(documentoXMLPersonajes, new FileWriter(archivoXMLPersonajes));
	}
	
	public static LinkedList<Personaje> obtenerPersonajesDeArchivo() throws JDOMException, IOException{
		LinkedList<Personaje> personajes = new LinkedList<Personaje>();
		Document documentoXMLPersonajes = (Document) new SAXBuilder().build(new File(RUTA_ARCHIVOS+File.separator+ARCHIVO_XML_PERSONAJES));
		Element nodoRaizXML = documentoXMLPersonajes.getRootElement();
		List<Element> listaElementosPersonajes = (List<Element>) nodoRaizXML.getChildren("personaje");
		for (Element elementoPersonaje:listaElementosPersonajes) {
			Personaje personaje = new Personaje();   
			personaje.setNombre(elementoPersonaje.getAttribute("nombre").getValue());
			personaje.setDatosImagenBase64(elementoPersonaje.getAttribute("datosImagenBase64").getValue());
			
			LinkedList<String> caracteristicasPersonaje = new LinkedList<String>();
			
			List<Element> listaElementosCaracteristicas = (List<Element>) elementoPersonaje.getChildren("caracteristica");
			for(Element elementoCaracteristicaPersonaje:listaElementosCaracteristicas) {
				caracteristicasPersonaje.add(elementoCaracteristicaPersonaje.getText());
			}
			
			personaje.setCaracteristicas(caracteristicasPersonaje);
			personajes.add(personaje);
		  
		}
		
		return personajes;
	}	
	
	
}
