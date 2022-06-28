package br.com.als.schema;

import java.util.Objects;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

// @ProtoDoc("@Indexed")
public class SalarioCDC {


   private Integer cd_cli;
   private Integer anoMes;
   private Float renda  ;
   private Long ts_atl;

   @ProtoFactory
   public SalarioCDC(Integer cd_cli, Integer anoMes, Float renda, Long ts_atl) {
      this.cd_cli = Objects.requireNonNull(cd_cli);
      this.anoMes = Objects.requireNonNull(anoMes);
      this.renda = Objects.requireNonNull(renda);
      this.ts_atl = Objects.requireNonNull(ts_atl);
   }


   @ProtoField(1)
   public Integer getCd_cli() {
      return cd_cli;
   }


   public void setCd_cli(Integer cd_cli) {
      this.cd_cli = cd_cli;
   }



   @ProtoField(2)
   // @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.YES)")

   public Integer getAnoMes() {
      return anoMes;
   }

   public void setAnoMes(Integer anoMes) {
      this.anoMes = anoMes;
   }


   @ProtoField(3)
   @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.NO, store = Store.YES)")
   
   public Float getRenda() {
      return renda;
   }

   public void setRenda(Float renda) {
      this.renda = renda;
   }


   @ProtoField(4)
   // @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.NO, store = Store.YES)")
   public Long getTs_atl() {
      return ts_atl;
   }

   public void setTs_atl(Long ts_atl) {
      this.ts_atl = ts_atl;
   }



   @Override
   public String toString() {
      return "SalarioCDC [cd_cli=" + cd_cli + ", anoMes=" + anoMes + ", renda=" + renda + ", ts_atl=" + ts_atl + "]";
   }


   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      SalarioCDC that = (SalarioCDC) o;
      return Objects.equals(cd_cli, that.cd_cli) && Objects.equals(anoMes, that.anoMes)
            && Objects.equals(renda, that.renda) && Objects.equals(ts_atl, that.ts_atl);
   }

   @Override
   public int hashCode() {
      return Objects.hash(cd_cli, anoMes, renda, ts_atl);
   }

}
