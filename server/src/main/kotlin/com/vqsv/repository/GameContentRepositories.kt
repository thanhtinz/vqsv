package com.vqsv.repository

import com.vqsv.entity.MapWarp
import com.vqsv.entity.Npc
import com.vqsv.entity.NpcEnemyTemplate
import com.vqsv.entity.Skill
import com.vqsv.entity.TrainerPartyMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NpcRepository : JpaRepository<Npc, Short> {
    fun findByMapId(mapId: Short): List<Npc>
}

@Repository
interface NpcEnemyTemplateRepository : JpaRepository<NpcEnemyTemplate, Short> {
    fun findByMapId(mapId: Short): List<NpcEnemyTemplate>
}

@Repository
interface MapWarpRepository : JpaRepository<MapWarp, Int> {
    fun findByFromMap(fromMap: Short): List<MapWarp>
}

@Repository
interface SkillRepository : JpaRepository<Skill, Short> {
    fun findByElement(element: Short): List<Skill>
}

@Repository
interface TrainerPartyRepository : JpaRepository<TrainerPartyMember, Int> {
    fun findByNpcIdOrderBySlotAsc(npcId: Short): List<TrainerPartyMember>
}
