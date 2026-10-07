"""Read-only Java ABI risk scanner. Usage: python tools/audit_addons.py MODS_DIR --output report.json"""
import argparse,io,json,pathlib,re,struct,zipfile
PREFIXES=('io/github/apace100/apoli/','io/github/apace100/origins/','io/github/apace100/calio/')
def references(data):
    if data[:4]!=b'\xca\xfe\xba\xbe':return []
    count=struct.unpack_from('>H',data,8)[0];offset=10;pool={};classes=[];descriptors=[];index=1
    while index<count:
        tag=data[offset];offset+=1
        if tag==1:
            size=struct.unpack_from('>H',data,offset)[0];offset+=2
            pool[index]=data[offset:offset+size].decode('utf-8',errors='replace');offset+=size
        elif tag==7:classes.append(struct.unpack_from('>H',data,offset)[0]);offset+=2
        elif tag==12:descriptors.append(struct.unpack_from('>H',data,offset+2)[0]);offset+=4
        elif tag in (3,4,9,10,11,17,18):offset+=4
        elif tag in (5,6):offset+=8;index+=1
        elif tag in (8,16,19,20):offset+=2
        elif tag==15:offset+=3
        else:raise ValueError('Unknown constant pool tag '+str(tag))
        index+=1
    refs=set()
    # Every UTF8 entry: annotation values (@Mixin targets), signatures and reflective strings never pass through
    # a Class or NameAndType entry. Dotted names are normalized to internal form.
    for text in pool.values():
        text=text.replace('.','/')
        for prefix in PREFIXES:
            for value in re.findall(re.escape(prefix)+r'[\w/$]+',text):refs.add(value)
    return sorted(refs)
def inspect(blob,label,depth=0):
    with zipfile.ZipFile(blob) as archive:
        meta=json.loads(archive.read('fabric.mod.json')) if 'fabric.mod.json' in archive.namelist() else {}
        rows=[];errors=[]
        for name in archive.namelist():
            if name.endswith('.class'):
                try:
                    refs=references(archive.read(name))
                    if refs:rows.append({'class':name,'legacy_references':refs})
                except Exception as error:errors.append({'class':name,'error':str(error)})
        result=[{'archive':label,'mod_id':meta.get('id'),'depends':meta.get('depends',{}),'legacy_api_classes':rows,'scan_errors':errors}]
        if depth<4:
            for entry in meta.get('jars',[]):
                name=entry['file']
                if name in archive.namelist():result+=inspect(io.BytesIO(archive.read(name)),label+'!'+name,depth+1)
        return result
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('mods',type=pathlib.Path);parser.add_argument('--output',type=pathlib.Path,required=True);args=parser.parse_args()
    rows=[]
    for jar in sorted(args.mods.glob('*.jar')):
        try:rows.extend(inspect(jar,jar.name))
        except Exception as error:rows.append({'archive':jar.name,'error':str(error)})
    report={'scope':'Bytecode API references and declared dependencies are porting candidates, not proof that an optional path executes. No jars or loader overrides are changed.','archives':rows}
    args.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    affected=[r for r in rows if r.get('legacy_api_classes')]
    print(len(affected),'archive(s) refer to legacy APIs out of',len(rows),'scanned archives')
    for row in affected:print(row['archive'],len(row['legacy_api_classes']),'class(es)')
