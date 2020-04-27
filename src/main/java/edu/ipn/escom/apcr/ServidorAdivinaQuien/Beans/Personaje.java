package edu.ipn.escom.apcr.ServidorAdivinaQuien.Beans;

import java.io.Serializable;
import java.util.LinkedList;

public class Personaje implements Serializable{

	private static final long serialVersionUID = 7636124386164314996L;
	
	private String nombre;
	private LinkedList<String> caracteristicas;
	private String datosImagenBase64;
	
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public LinkedList<String> getCaracteristicas() {
		return caracteristicas;
	}
	public void setCaracteristicas(LinkedList<String> caracteristicas) {
		this.caracteristicas = caracteristicas;
	}
	public String getDatosImagenBase64() {
		return datosImagenBase64;
	}
	public void setDatosImagenBase64(String datosImagenBase64) {
		this.datosImagenBase64 = datosImagenBase64;
	}
	
}
