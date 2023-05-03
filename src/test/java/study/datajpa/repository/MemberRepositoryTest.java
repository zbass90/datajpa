package study.datajpa.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;
import study.datajpa.entity.Team;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(value = false)
class MemberRepositoryTest {
    @Autowired MemberRepository memberRepository;
    @Autowired TeamRepository teamRepository;

    @Test
    public void testMember(){
        Member member = new Member("memberA");
        Member saveMember = memberRepository.save(member);

        Member findMember = memberRepository.findById(saveMember.getId()).get();

        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);
    }

    @Test
    public void basicCRUD(){
        Member member1 = new Member("member1");
        Member member2 = new Member("member2");
        memberRepository.save(member1);
        memberRepository.save(member2);

        //단건 조회 검증
        Member findMember1 = memberRepository.findById(member1.getId()).get();
        Member findMember2 = memberRepository.findById(member2.getId()).get();
        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        member1.setUsername("haha");

        //리스트 조회 검증
        List<Member> all = memberRepository.findAll();
        assertThat(all.size()).isEqualTo(2);

        //카운트 검증
        long count = memberRepository.count();
        assertThat(count).isEqualTo(2);

        //삭제 검증
        memberRepository.delete(member1);
        memberRepository.delete(member2);

        long deletedCount = memberRepository.count();
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    public void findByUsernameAndAgeGreaterThan() {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);
        List<Member> result =
                memberRepository.findByUsernameAndAgeGreaterThan("AAA", 15);
        assertThat(result.get(0).getUsername()).isEqualTo("AAA");
        assertThat(result.get(0).getAge()).isEqualTo(20);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void testQuery() {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);
        List<Member> result =
                memberRepository.findUser("AAA", 10);
        assertThat(result.get(0)).isEqualTo(m1);
    }

    @Test
    public void findUsernameList() {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);

        List<String> usernameList = memberRepository.findUsernameList();
        assertThat(usernameList.get(0)).isEqualTo(m1.getUsername());
        assertThat(usernameList.get(1)).isEqualTo(m2.getUsername());
    }

    @Test
    public void findMemberDto() {
        Team team = new Team("teamA");
        teamRepository.save(team);

        Member m1 = new Member("AAA", 10);
        m1.setTeam(team);
        memberRepository.save(m1);


        List<MemberDto> memberDto = memberRepository.findMemberDto();
        assertThat(memberDto.get(0).getUsername()).isEqualTo(m1.getUsername());
        assertThat(memberDto.get(0).getTeamname()).isEqualTo(m1.getTeam().getName());
    }

    @Test
    public void findByNames() {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);

        List<Member> usernameList = memberRepository.findByNames(Arrays.asList("AAA","BBB"));

        assertThat(usernameList.get(0).getUsername()).isEqualTo(m1.getUsername());
        assertThat(usernameList.get(1).getUsername()).isEqualTo(m2.getUsername());
    }

    @Test
    public void returnType() {
        /*

        *   기존 JPA는 NoResultException을
        *   Spring Data JPA에서는 NoResultException을 try...catch로 null을 반환시킴
        *   Exception VS null => null이 좋고 Optional로 사용하는걸 권장. (java 1.8 부터)
        *   Optional getOrElse , OrElse 사용
        *
        *   값이 여러개 예를 들어 AAA, AAA 2개 이상인데 하나 가지고 올때
        *   Optional과 관계없이 Exception을 뱉음
        *   NonUniqueResultException(JPA에서 뱉음) =>
        *   Sptring Data JPA가 IncorrectResultSizeDataAccessException(공통 DB이슈) 변환해서 뱉음
        *
        * */
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);

        List<Member> aaa = memberRepository.findListByUsername("AAA");
        assertThat(m1.getUsername()).isEqualTo(aaa.get(0).getUsername());

        Member findMember = memberRepository.findMemberByUsername("AAA");
        assertThat(m1.getUsername()).isEqualTo(findMember.getUsername());

        Optional<Member> findOptionalMember = memberRepository.findOptionalByUsername("AAA");
        assertThat(m1.getUsername()).isEqualTo(findOptionalMember.get().getUsername());
    }

    @Test
    public void paging(){
        //given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));

        int age = 10;
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.Direction.DESC, "username");

        //when
        Page<Member> page = memberRepository.findByAge(age, pageRequest); //반환 타입에 따라 total 쿼리 발생에 영향을 줌

        // entity -> Dto 변환
        Page<MemberDto> toMap = page.map(member -> new MemberDto(member.getId(), member.getUsername(), null));
        //then

        List<Member> content = page.getContent();
        long totalElements = page.getTotalElements();
        assertThat(content.size()).isEqualTo(3); // 페이지에 있는 컨텐츠 갯수
        assertThat(totalElements).isEqualTo(5); // 총 컨텐츠 갯수
        assertThat(page.getNumber()).isEqualTo(0); //현재 페이지 번호
        assertThat(page.getTotalPages()).isEqualTo(2); //전체 페이지 갯수
        assertThat(page.isFirst()).isTrue(); // 첫번째 페이지인지
        assertThat(page.hasNext()).isTrue(); // 다음 페이지가 있는지
        assertThat(page.isLast()).isFalse(); // 마지막 페이지인지
    }

    @Test
    public void slicePaging(){
        //given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));

        int age = 10;
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.Direction.DESC, "username");

        //when
        Slice<Member> page = memberRepository.findByAge(age, pageRequest); //반환 타입에 따라 total 쿼리 발생에 영향을 줌
        //then

        List<Member> content = page.getContent();
        assertThat(content.size()).isEqualTo(3); // 페이지에 있는 컨텐츠 갯수
        assertThat(page.getNumber()).isEqualTo(0); //현재 페이지 번호
        assertThat(page.isFirst()).isTrue(); // 첫번째 페이지인지
        assertThat(page.hasNext()).isTrue(); // 다음 페이지가 있는지
        assertThat(page.isLast()).isFalse(); // 마지막 페이지인지
    }

    @Test
    public void bulkUpdate(){
        // 벌크연산은 DB에 바로 넣는다
        // given 영속성 컨텍스트에 있는거지 디비에 있지 않음
        //given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 19));
        memberRepository.save(new Member("member3", 20));
        memberRepository.save(new Member("member4", 21));
        memberRepository.save(new Member("member5", 40));

        //when
        int resultCount = memberRepository.bulkAgePlus(20);

        //then
        assertThat(resultCount).isEqualTo(3);

    }

    @Test
    public void findMemberLazy(){
        //given
    }
}