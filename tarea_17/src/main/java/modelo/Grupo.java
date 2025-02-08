package modelo;

import java.io.Serializable;
import java.util.List;

public class Grupo implements Serializable {

	private static final long serialVersionUID = 1074847470373142375L;
	private int numeroGrupo; // Este campo será asignado por la base de datos
	private String nombreGrupo;
	private List<Alumno> alumnos;

	// Constructor vacío:
	public Grupo() {
	}

	// Constructor con nombreGrupo, sin numeroGrupo (lo gestionaremos desde la BD)
	public Grupo(String nombreGrupo) {
		this.nombreGrupo = nombreGrupo;
	}

	// Constructor para cuando obtenemos el grupo de la BD (con numeroGrupo)
	public Grupo(int numeroGrupo, String nombreGrupo) {
		this.numeroGrupo = numeroGrupo;
		this.nombreGrupo = nombreGrupo;
	}

	// Getter y Setter para numeroGrupo
	public int getNumeroGrupo() {
		return numeroGrupo;
	}

	public void setNumeroGrupo(int numeroGrupo) {
		this.numeroGrupo = numeroGrupo;
	}

	// Getter y Setter para nombreGrupo
	public String getNombreGrupo() {
		return nombreGrupo;
	}

	public void setNombreGrupo(String nombreGrupo) {
		this.nombreGrupo = nombreGrupo;
	}
	
	public List<Alumno> getAlumnos() {
        return alumnos;
    }

    public void setAlumnos(List<Alumno> alumnos) {
        this.alumnos = alumnos;
    }
}
