import {createMixer} from './countertop-model.js';
import * as T from 'three';
import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {createHash} from 'node:crypto';
const m=createMixer(),parts=[];
for(const [id,g] of Object.entries(m.parts)){
  const positions=[],normals=[],colours=[];
  for(const mesh of g.children){
    const geom=mesh.geometry.index?mesh.geometry.toNonIndexed():mesh.geometry;
    mesh.updateMatrix();const normalMatrix=new T.Matrix3().getNormalMatrix(mesh.matrix);
    for(let i=0;i<geom.attributes.position.count;i++){
      const p=new T.Vector3().fromBufferAttribute(geom.attributes.position,i).applyMatrix4(mesh.matrix);
      const n=new T.Vector3().fromBufferAttribute(geom.attributes.normal,i).applyMatrix3(normalMatrix).normalize();
      const c=new T.Color().fromBufferAttribute(geom.attributes.color,i).convertLinearToSRGB();
      positions.push(...p.toArray().map(v=>+v.toFixed(5)));normals.push(...n.toArray().map(v=>+v.toFixed(5)));
      colours.push((0xff000000|(Math.round(c.r*255)<<16)|(Math.round(c.g*255)<<8)|Math.round(c.b*255))>>>0);
    }
  }
  parts.push({id,origin:g.position.toArray(),positions,normals,colours});
}
const source=await readFile(new URL('./countertop-model.js',import.meta.url));
const asset={schema:1,sourceSha256:createHash('sha256').update(source).digest('hex'),units:'16 model pixels per block',bowlContents:'empty',parts};
const folder=new URL('../../src/main/resources/assets/hearth_horizons_qol/models/mesh/',import.meta.url);
await mkdir(folder,{recursive:true});await writeFile(new URL('manual_mixer.json',folder),JSON.stringify(asset));
console.log('Exported '+parts.length+' parts, '+parts.reduce((n,p)=>n+p.positions.length/9,0)+' triangles');
