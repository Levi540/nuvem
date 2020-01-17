package repository;

import controller.JPAListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import model.Usuario;

public class UsuarioRepository {
    
    private EntityManager em;
    
    public UsuarioRepository() {
        em = JPAListener.createEntityManager();
    }
    
    public void addUsuario(Usuario usuario) {
        usuario.setDataCadastro(new Date());
        usuario.setId(UUID.randomUUID().toString());
        em.getTransaction().begin();
        try {
            em.persist(usuario);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.getStackTrace();
            em.getTransaction().rollback();
        }
    }
    
    public Usuario getUsuario(String email) {
        Usuario usuario = new Usuario();
        try {
            usuario =
                em.createQuery("select u from Usuario u where u.email like '" +email+ "'", Usuario.class)
                        .getSingleResult();
        } catch (NoResultException e) {
            e.getStackTrace();
        }
        return usuario;
    }
    
    public boolean verificarContaExiste(String email) {
        try {
            return em.createQuery("select u from Usuario u where u.email like '" +email+ "'", Usuario.class)
                        .getSingleResult().getEmail().equals(email);
        } catch (NoResultException e) {
            e.getStackTrace();
        }
        return false;
    }
    
    public void alterar(Usuario usuario) {
        try {
            em.getTransaction().begin();
            em.merge(usuario);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.getStackTrace();
            em.getTransaction().rollback();
        }
    }
}
