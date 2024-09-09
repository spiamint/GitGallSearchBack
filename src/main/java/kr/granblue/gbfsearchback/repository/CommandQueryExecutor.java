package kr.granblue.gbfsearchback.repository;

import kr.granblue.gbfsearchback.domain.DcBoardEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * supabase web 에서 할수 없는 쿼리 실행용
 */
public interface CommandQueryExecutor extends JpaRepository<DcBoardEmbedding, Long> {

    @Query(nativeQuery = true, value = "VACUUM full")
    void vacuum();

    @Modifying
    @Query(value = "alter role authenticator set statement_timeout = '300s'", nativeQuery = true)
    void alterTimeout();

    @Modifying
    @Query(value = "set statement_timeout to 3000", nativeQuery = true)
    void alterTimeout2();

}
