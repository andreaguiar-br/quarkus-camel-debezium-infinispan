package br.com.als.schema;

import java.util.Objects;
import java.util.Set;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoDoc("@Indexed")
public class ClienteCDC {

   private Integer cd_cli;
   private String nm_cli;
   private Long nr_cpf;
   private Long ts_atl;
   private Set<SalarioCDC> salarios;

   @ProtoFactory
   public ClienteCDC(Integer cd_cli, String nm_cli, Long nr_cpf, Long ts_atl, Set<SalarioCDC> salarios) {
      this.cd_cli = Objects.requireNonNull(cd_cli);
      this.nm_cli = Objects.requireNonNull(nm_cli);
      this.nr_cpf = Objects.requireNonNull(nr_cpf);
      this.ts_atl = Objects.requireNonNull(ts_atl);
      this.salarios = salarios;
   }

   public ClienteCDC() {
   }

   @ProtoField(1)
   public Integer getCd_cli() {
      return cd_cli;
   }

   public void setCd_cli(Integer cd_cli) {
      this.cd_cli = cd_cli;
   }

   @ProtoField(2)
   @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.YES)")

   public String getNm_cli() {
      return nm_cli;
   }

   public void setNm_cli(String nm_cli) {
      this.nm_cli = nm_cli;
   }

   @ProtoField(3)
   @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.NO, store = Store.YES)")

   public Long getNr_cpf() {
      return nr_cpf;
   }

   public void setNr_cpf(Long nr_cpf) {
      this.nr_cpf = nr_cpf;
   }

   @ProtoField(4)
   // @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.NO, store = Store.YES)")
   public Long getTs_atl() {
      return ts_atl;
   }

   public void setTs_atl(Long ts_atl) {
      this.ts_atl = ts_atl;
   }

   @ProtoField(5)
   public Set<SalarioCDC> getSalarios() {
      return salarios;
   }

   public void setSalarios(Set<SalarioCDC> salarios) {
      if (salarios.stream().allMatch(sal -> sal.getCd_cli() == this.cd_cli)) {
         this.salarios = salarios;
      }
   }

   public void addOrUpdateSalario(SalarioCDC salario) {
      if (this.cd_cli == salario.getCd_cli()) {

       this.salarios
             .removeIf(sal -> sal.getCd_cli().intValue() == salario.getCd_cli().intValue() 
                           && sal.getAnoMes().intValue() == salario.getAnoMes().intValue());
         this.salarios.add(salario);
      }

   }

   @Override
   public String toString() {
      return "ClienteCDC [cd_cli=" + cd_cli + ", nm_cli=" + nm_cli + ", nr_cpf=" + nr_cpf + ", salarios=" + salarios
            + ", ts_atl=" + ts_atl + "]";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      ClienteCDC that = (ClienteCDC) o;
      return Objects.equals(cd_cli, that.cd_cli) && Objects.equals(nm_cli, that.nm_cli)
            && Objects.equals(nr_cpf, that.nr_cpf) && Objects.equals(ts_atl, that.ts_atl)
            && Objects.equals(salarios, that.salarios);
   }

   @Override
   public int hashCode() {
      return Objects.hash(cd_cli, nm_cli, nr_cpf, ts_atl, salarios);
   }

}
