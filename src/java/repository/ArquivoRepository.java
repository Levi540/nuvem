package repository;

import controller.JPAListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import model.Arquivo;

public class ArquivoRepository {

    private EntityManager em;

    public ArquivoRepository() {
        em = JPAListener.createEntityManager();
    }

    public void addArquivo(Arquivo arquivo) {
        EntityTransaction tx = em.getTransaction();
        arquivo.setDataCriacao(new Date());
        arquivo.setId(UUID.randomUUID().toString());
        tx.begin();
        try {
            em.persist(arquivo);
            tx.commit();
        } catch (Exception e) {
            e.getStackTrace();
            tx.rollback();
        }
    }

    public List<Arquivo> getArquivos(String diretorio, boolean inicio) {
        List<Arquivo> arquivos  = new ArrayList<>();
        try {
            if (inicio) {
                arquivos
                        = em.createQuery("select a from Arquivo a where a.diretorio like '"
                                + diretorio + "'", Arquivo.class)
                        .getResultList();
            } else {
                arquivos
                        = em.createQuery("select a from Arquivo a where a.diretorio like \""
                                + diretorio + "\"", Arquivo.class)
                        .getResultList();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println(diretorio);
        return arquivos;
    }
    
    public List<Arquivo> getArquivosToDownload(String diretorio) {
        List<Arquivo> arquivos = 
                em.createQuery("select a from Arquivo a where a.diretorio like '" 
                        + diretorio + "%' and a.tipo not like 'Pasta'", Arquivo.class)
                .getResultList();
        return arquivos;
    }

    public Arquivo getArquivo(String id) {
        Arquivo arquivo = em.find(Arquivo.class, id);
        return arquivo;
    }

    public boolean verificarArquivoExiste(String diretorio, String nome, String tipo) {
        Arquivo arquivo;
        try {
            arquivo = 
                em.createQuery("select a from Arquivo a where "
                        + "a.diretorio = :diretorio and a.nome = :nome "
                        + "and a.tipo = :tipo", Arquivo.class)
                        .setParameter("diretorio", diretorio)
                        .setParameter("nome", nome).setParameter("tipo", tipo).getSingleResult();
        } catch (NoResultException e) {
            return false;
        }
        return true;
    }
    
    public void excluir(Arquivo arquivo) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            em.remove(arquivo);
            tx.commit();
        } catch (Exception e) {
            e.getStackTrace();
            tx.rollback();
        }
    }
    
    public void excluirPasta(String diretorio) {
        List<Arquivo> arquivos = new ArrayList<>();
        EntityTransaction tx = em.getTransaction();
        
        arquivos = em.createQuery("select a from Arquivo a where a.diretorio like '" 
                + diretorio + "%'", Arquivo.class).getResultList();
        
        try {
            for (int i = 0; i < arquivos.size(); i++) {
                tx.begin();
                em.remove(arquivos.get(i));
                tx.commit();
            }
        } catch (Exception e) {
            e.getStackTrace();
            tx.rollback();
        }
    }
}
